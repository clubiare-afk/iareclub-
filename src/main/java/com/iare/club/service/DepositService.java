package com.iare.club.service;

import com.iare.club.entity.AppUser;
import com.iare.club.entity.Deposit;
import com.iare.club.entity.Wallet;
import com.iare.club.entity.WalletTransaction;
import com.iare.club.repository.DepositRepository;
import com.iare.club.repository.WalletRepository;
import com.iare.club.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {
    private final DepositRepository deposits;
    private final WalletRepository wallets;
    private final WalletTransactionRepository transactions;
    private final WalletService walletService;
    private final Clock clock;
    private final boolean demoEnabled;
    private final RateLimitService limits;

    public DepositService(DepositRepository deposits, WalletRepository wallets,
            WalletTransactionRepository transactions, WalletService walletService, Clock clock,
            RateLimitService limits,
            @Value("${moneymint.demo-payments.enabled:false}") boolean demoEnabled) {
        this.deposits = deposits;
        this.wallets = wallets;
        this.transactions = transactions;
        this.walletService = walletService;
        this.clock = clock;
        this.limits = limits;
        this.demoEnabled = demoEnabled;
    }

    @Transactional
    public Deposit depositInstantly(AppUser user, BigDecimal amount) {
        limits.check("deposit:" + user.getId(), 10);
        validateAmount(amount);
        String reference = "DPT-" + UUID.randomUUID();
        Deposit deposit = deposits.save(
                new Deposit(user, amount.setScale(2), reference, "MANUAL_DEMO", clock.instant()));
        Wallet wallet = walletService.lock(user);
        wallet.creditDeposit(deposit.getAmount());
        deposit.markSuccessful(clock.instant(),wallet.getDepositBalance());
        wallets.save(wallet);
        transactions.save(new WalletTransaction(user, WalletTransaction.Type.DEPOSIT,
                WalletTransaction.Status.SUCCESSFUL, deposit.getAmount(), wallet.getDepositBalance(),
                reference, "Temporary direct deposit credited by the server", clock.instant()));
        return deposit;
    }

    @Transactional
    public Deposit create(AppUser user, BigDecimal amount) {
        limits.check("deposit:" + user.getId(), 10);
        validateAmount(amount);
        String reference = "DPT-" + UUID.randomUUID();
        return deposits.save(new Deposit(user, amount.setScale(2), reference, "DEMO", clock.instant()));
    }

    @Transactional
    public Deposit createPayU(AppUser user, BigDecimal amount) {
        limits.check("payu-create:" + user.getId(), 10);
        validateAmount(amount);
        String reference = "MM" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        return deposits.save(new Deposit(user, amount.setScale(2, RoundingMode.UNNECESSARY), reference, "PAYU", clock.instant()));
    }

    @Transactional
    public Deposit applyPayUVerification(PayUClient.Verification verification) {
        Deposit deposit = deposits.findByPaymentReferenceForUpdate(verification.transactionId())
                .orElseThrow(() -> new FinancialException("Deposit not found"));
        if (deposit.getStatus() == Deposit.Status.SUCCESSFUL) return deposit;
        if (verification.amount() != null && verification.amount().compareTo(deposit.getAmount()) != 0)
            throw new FinancialException("PayU amount mismatch; payment was not credited");
        Instant now = clock.instant();
        if (verification.captured()) {
            if (verification.amount() == null) throw new FinancialException("PayU did not return a verifiable amount");
            Wallet wallet = walletService.lock(deposit.getUser());
            wallet.creditDeposit(deposit.getAmount());
            if (deposit.markSuccessful(now, wallet.getDepositBalance())) {
                deposit.updateGateway(verification.payuPaymentId(), verification.paymentMethod(), verification.unmappedStatus(), verification.message(), verification.bankReference(), now);
                wallets.save(wallet);
                if (!transactions.existsByReference(deposit.getPaymentReference())) transactions.save(new WalletTransaction(deposit.getUser(), WalletTransaction.Type.DEPOSIT,
                        WalletTransaction.Status.SUCCESSFUL, deposit.getAmount(), wallet.getDepositBalance(), deposit.getPaymentReference(), "PayU payment verified and credited", now));
            }
        } else if (verification.pending() || "not_found".equalsIgnoreCase(verification.status())) {
            deposit.markPending(verification.payuPaymentId(), verification.paymentMethod(), verification.unmappedStatus(), verification.message(), verification.bankReference(), now);
        } else {
            deposit.markFailed(verification.cancelled(), verification.payuPaymentId(), verification.paymentMethod(), verification.unmappedStatus(), verification.message(), verification.bankReference(), now);
        }
        return deposits.save(deposit);
    }

    @Transactional
    public Deposit confirmDemo(AppUser user, String reference) {
        if (!demoEnabled) throw new FinancialException("Demo payments are disabled");
        limits.check("deposit-confirm:" + user.getId(), 10);
        Deposit deposit = deposits.findByPaymentReferenceForUpdate(reference)
                .orElseThrow(() -> new FinancialException("Deposit not found"));
        if (!deposit.getUser().getId().equals(user.getId())) throw new FinancialException("Deposit not found");
        if (deposit.getStatus() == Deposit.Status.SUCCESSFUL) return deposit;
        if (deposit.getStatus() != Deposit.Status.PENDING)
            throw new FinancialException("This deposit cannot be confirmed");
        Wallet wallet = walletService.lock(user);
        wallet.creditDeposit(deposit.getAmount());
        deposit.markSuccessful(clock.instant(),wallet.getDepositBalance());
        wallets.save(wallet);
        if (!transactions.existsByReference(reference)) {
            transactions.save(new WalletTransaction(user, WalletTransaction.Type.DEPOSIT,
                    WalletTransaction.Status.SUCCESSFUL, deposit.getAmount(), wallet.getDepositBalance(),
                    reference, "Demo deposit credited after server confirmation", clock.instant()));
        }
        return deposit;
    }

    @Transactional(readOnly = true)
    public List<Deposit> history(AppUser user) {
        return deposits.findTop50ByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public boolean isDemoEnabled() { return demoEnabled; }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0)
            throw new FinancialException("Deposit amount must be at least ₹1");
    }

    public static class FinancialException extends RuntimeException {
        public FinancialException(String message) { super(message); }
    }
}
