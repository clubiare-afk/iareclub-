package com.iare.club.service;

import com.iare.club.dto.BankAccountRequest;
import com.iare.club.entity.*;
import com.iare.club.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawalService {
    private static final Duration COOLDOWN = Duration.ofHours(24);
    private final BankAccountRepository banks;
    private final WithdrawalRepository withdrawals;
    private final WalletRepository wallets;
    private final WalletService walletService;
    private final WalletTransactionRepository transactions;
    private final AdminAuditLogRepository audits;
    private final BankEncryptionService encryption;
    private final Clock clock;
    private final BigDecimal minimum;
    private final RateLimitService limits;

    public WithdrawalService(BankAccountRepository banks, WithdrawalRepository withdrawals,
            WalletRepository wallets, WalletService walletService,
            WalletTransactionRepository transactions, AdminAuditLogRepository audits,
            BankEncryptionService encryption, Clock clock, RateLimitService limits,
            @Value("${moneymint.withdrawal.minimum:100.00}") BigDecimal minimum) {
        this.banks = banks; this.withdrawals = withdrawals; this.wallets = wallets;
        this.walletService = walletService; this.transactions = transactions; this.audits = audits;
        this.encryption = encryption; this.clock = clock; this.limits = limits; this.minimum = minimum;
    }

    @Transactional
    public BankAccount saveBank(AppUser user, BankAccountRequest dto) {
        limits.check("bank:" + user.getId(), 5);
        String number = dto.getAccountNumber().strip();
        String ifsc = dto.getIfscCode().strip().toUpperCase(Locale.ROOT);
        BankAccount bank = banks.findByUserId(user.getId()).orElseGet(() -> new BankAccount(user));
        bank.updateDetails(dto.getAccountHolderName().strip(), dto.getBankName().strip(),
                encryption.encrypt(number), number.substring(number.length() - 4), encryption.encrypt(ifsc));
        return banks.save(bank);
    }

    @Transactional
    public WithdrawalRequest request(AppUser user, BigDecimal amount) {
        limits.check("withdraw:" + user.getId(), 5);
        if (amount == null || amount.compareTo(minimum) < 0)
            throw new WithdrawalException("Minimum withdrawal is ₹" + minimum);
        BankAccount bank = banks.findByUserId(user.getId())
                .orElseThrow(() -> new WithdrawalException("Add a bank account before requesting a withdrawal"));

        // The wallet row serializes requests for this user, preventing concurrent cooldown bypasses.
        Wallet wallet = walletService.lock(user);
        Instant now = clock.instant();
        Optional<Instant> next = nextWithdrawalAt(user);
        if (next.isPresent() && now.isBefore(next.get())) throw new WithdrawalCooldownException(next.get());
        if (wallet.getRewardBalance().compareTo(amount) < 0)
            throw new WithdrawalException("Insufficient available reward balance");

        String reference = "WDL-" + UUID.randomUUID();
        wallet.reserveReward(amount);
        WithdrawalRequest request = withdrawals.save(new WithdrawalRequest(user, bank, amount, reference, now));
        wallets.save(wallet);
        transactions.save(new WalletTransaction(user, WalletTransaction.Type.WITHDRAWAL_REQUEST,
                WalletTransaction.Status.PENDING, amount.negate(), wallet.getRewardBalance(), reference,
                "Withdrawal requested and funds reserved", now));
        return request;
    }

    @Transactional
    public WithdrawalRequest transition(long id, WithdrawalRequest.Status next, String remarks,
            String payoutReference, String admin) {
        WithdrawalRequest request = withdrawals.findByIdForUpdate(id)
                .orElseThrow(() -> new WithdrawalException("Withdrawal not found"));
        WithdrawalRequest.Status previous = request.getStatus();
        validateTransition(previous, next, payoutReference);
        if ((next == WithdrawalRequest.Status.FAILED || next == WithdrawalRequest.Status.REFUNDED)
                && !request.isFundsReleased()) {
            Wallet wallet = walletService.lock(request.getUser());
            wallet.refundWithdrawal(request.getAmount()); wallets.save(wallet); request.markFundsReleased();
            transactions.save(new WalletTransaction(request.getUser(), WalletTransaction.Type.WITHDRAWAL_REFUND,
                    WalletTransaction.Status.REFUNDED, request.getAmount(), wallet.getRewardBalance(),
                    "REF-" + request.getReference(), "Reserved withdrawal funds returned", clock.instant()));
        } else if (next == WithdrawalRequest.Status.SUCCESSFUL && !request.isFundsReleased()) {
            Wallet wallet = walletService.lock(request.getUser());
            wallet.completeWithdrawal(request.getAmount()); wallets.save(wallet); request.markFundsReleased();
            transactions.save(new WalletTransaction(request.getUser(), WalletTransaction.Type.WITHDRAWAL_SUCCESS,
                    WalletTransaction.Status.SUCCESSFUL, request.getAmount().negate(), wallet.getRewardBalance(),
                    "PAID-" + request.getReference(), "Withdrawal completed after verified payout", clock.instant()));
        }
        request.transition(next, remarks, payoutReference, clock.instant()); withdrawals.save(request);
        audits.save(new AdminAuditLog(admin, "WITHDRAWAL", id, "STATUS_CHANGE", previous.name(),
                next.name(), remarks, clock.instant()));
        return request;
    }

    private void validateTransition(WithdrawalRequest.Status from, WithdrawalRequest.Status to, String payout) {
        boolean valid = (from == WithdrawalRequest.Status.PENDING && (to == WithdrawalRequest.Status.PROCESSING
                || to == WithdrawalRequest.Status.SUCCESSFUL || to == WithdrawalRequest.Status.FAILED
                || to == WithdrawalRequest.Status.REFUNDED))
                || (from == WithdrawalRequest.Status.PROCESSING && (to == WithdrawalRequest.Status.SUCCESSFUL
                || to == WithdrawalRequest.Status.FAILED || to == WithdrawalRequest.Status.REFUNDED));
        if (!valid) throw new WithdrawalException("Invalid withdrawal status transition");
        if (to == WithdrawalRequest.Status.SUCCESSFUL && (payout == null || payout.isBlank()))
            throw new WithdrawalException("A verified payout reference is required for successful withdrawals");
    }

    @Transactional(readOnly = true) public Optional<BankAccount> bankFor(AppUser user) { return banks.findByUserId(user.getId()); }
    @Transactional(readOnly = true) public List<WithdrawalRequest> history(AppUser user) { return withdrawals.findTop50ByUserIdOrderByRequestedAtDesc(user.getId()); }
    @Transactional(readOnly = true) public Optional<Instant> nextWithdrawalAt(AppUser user) { return withdrawals.findFirstByUserIdOrderByRequestedAtDesc(user.getId()).map(item -> item.getRequestedAt().plus(COOLDOWN)); }
    public Instant now() { return clock.instant(); }
    public BigDecimal getMinimum() { return minimum; }
    public String fullAccountNumber(BankAccount bank) { return encryption.decrypt(bank.getAccountNumberEncrypted()); }
    public String ifsc(BankAccount bank) { return encryption.decrypt(bank.getIfscEncrypted()); }

    public static class WithdrawalException extends RuntimeException { public WithdrawalException(String message) { super(message); } }
    public static class WithdrawalCooldownException extends WithdrawalException {
        private final Instant availableAt;
        public WithdrawalCooldownException(Instant availableAt) { super("Only one withdrawal request is allowed every 24 hours"); this.availableAt = availableAt; }
        public Instant getAvailableAt() { return availableAt; }
    }
}
