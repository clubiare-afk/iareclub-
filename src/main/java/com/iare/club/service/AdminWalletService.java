package com.iare.club.service;

import com.iare.club.entity.*;
import com.iare.club.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminWalletService {
    private static final BigDecimal MAX_CREDIT = new BigDecimal("9999999999999999.99");
    private final UserRepository users;
    private final WalletService walletService;
    private final WalletRepository wallets;
    private final DepositRepository deposits;
    private final WalletTransactionRepository transactions;
    private final AdminCreditAuditRepository audits;
    private final Clock clock;

    public AdminWalletService(UserRepository users, WalletService walletService, WalletRepository wallets,
                              DepositRepository deposits, WalletTransactionRepository transactions,
                              AdminCreditAuditRepository audits, Clock clock) {
        this.users=users; this.walletService=walletService; this.wallets=wallets; this.deposits=deposits;
        this.transactions=transactions; this.audits=audits; this.clock=clock;
    }

    @Transactional
    public CreditResult credit(long userId, BigDecimal requestedAmount, String requestId, String adminIdentity) {
        BigDecimal amount = validateAmount(requestedAmount);
        String reference = referenceFor(requestId);
        AppUser user = users.findByIdForUpdate(userId).orElseThrow(() -> new CreditException("User was not found"));
        Wallet wallet = walletService.lock(user);

        var existing = transactions.findByReference(reference);
        if (existing.isPresent()) {
            WalletTransaction transaction = existing.get();
            if (!transaction.getUser().getId().equals(userId) || transaction.getAmount().compareTo(amount) != 0)
                throw new CreditException("This credit request has already been used");
            return new CreditResult(false, reference, amount, transaction.getBalanceBefore(), transaction.getBalanceAfter(), transaction.getCreatedAt());
        }

        BigDecimal before = wallet.getDepositBalance();
        wallet.creditDeposit(amount);
        BigDecimal after = wallet.getDepositBalance();
        Instant now = clock.instant();

        Deposit deposit = new Deposit(user, amount, reference, "ADMIN_CREDIT", now);
        deposit.markSuccessful(now, after);
        deposits.save(deposit);
        wallets.save(wallet);
        transactions.save(WalletTransaction.adminCredit(user, amount, before, after, reference, adminIdentity, now));
        audits.save(new AdminCreditAudit(userId, reference, amount, before, after, adminIdentity, now));
        return new CreditResult(true, reference, amount, before, after, now);
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new CreditException("Enter an amount greater than zero");
        try { amount = amount.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException exception) { throw new CreditException("Amount can have no more than two decimal places"); }
        if (amount.compareTo(MAX_CREDIT) > 0) throw new CreditException("Amount exceeds the supported wallet limit");
        return amount;
    }

    private String referenceFor(String requestId) {
        try {
            String value = UUID.fromString(requestId).toString().replace("-", "").toUpperCase();
            return "ADM" + value.substring(0, 24);
        } catch (RuntimeException exception) {
            throw new CreditException("Invalid or expired credit request");
        }
    }

    public record CreditResult(boolean credited, String transactionId, BigDecimal amount,
                               BigDecimal previousBalance, BigDecimal newBalance, Instant createdAt) {}
    public static class CreditException extends RuntimeException { public CreditException(String message) { super(message); } }
}
