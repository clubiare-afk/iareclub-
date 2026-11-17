package com.iare.club.service;

import com.iare.club.entity.AppUser;
import com.iare.club.entity.Deposit;
import com.iare.club.entity.Wallet;
import com.iare.club.entity.WalletTransaction;
import com.iare.club.repository.DepositRepository;
import com.iare.club.repository.WalletRepository;
import com.iare.club.repository.WalletTransactionRepository;
import com.iare.club.repository.ProductRepository;
import com.iare.club.repository.UserInvestmentRepository;
import com.iare.club.entity.InvestmentProduct;
import com.iare.club.entity.UserInvestment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    private final ProductRepository products;
    private final UserInvestmentRepository investments;
    private final ReferralService referrals;
    private final int rewardPeriodHours;

    public DepositService(DepositRepository deposits, WalletRepository wallets,
            WalletTransactionRepository transactions, WalletService walletService, Clock clock,
            RateLimitService limits, ProductRepository products, UserInvestmentRepository investments, ReferralService referrals,
            @Value("${moneymint.reward.period-hours:24}") int rewardPeriodHours,
            @Value("${moneymint.demo-payments.enabled:false}") boolean demoEnabled) {
        this.deposits = deposits;
        this.wallets = wallets;
        this.transactions = transactions;
        this.walletService = walletService;
        this.clock = clock;
        this.limits = limits;
        this.products=products;this.investments=investments;this.referrals=referrals;this.rewardPeriodHours=rewardPeriodHours;
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
    public Deposit createZapUpi(AppUser user, BigDecimal amount) { return createZapUpi(user,amount,"https://iareclub-production.up.railway.app"); }

    @Transactional
    public Deposit createZapUpi(AppUser user, BigDecimal amount, String returnOrigin) {
        limits.check("zapupi-create:" + user.getId(), 10);
        validateAmount(amount);
        String reference = "ZAP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        return deposits.save(new Deposit(user, amount.setScale(2, RoundingMode.UNNECESSARY), reference, "ZAPUPI", clock.instant(),returnOrigin));
    }

    @Transactional(readOnly = true)
    public Optional<Deposit> reusableZapUpi(AppUser user, BigDecimal amount) {
        validateAmount(amount);
        return deposits.findReusable(user.getId(), Deposit.Status.PENDING, "ZAPUPI",
                        clock.instant().minusSeconds(10 * 60), org.springframework.data.domain.PageRequest.of(0, 5))
                .stream().filter(d -> d.getAmount().compareTo(amount.setScale(2, RoundingMode.UNNECESSARY)) == 0).findFirst();
    }

    @Transactional
    public Deposit createZapUpiProduct(AppUser user,long productId,String requestReference,String returnOrigin){
        limits.check("zapupi-product:"+user.getId(),10);
        if(investments.existsByUserIdAndProductId(user.getId(),productId))throw new InvestmentService.DuplicatePurchaseException();
        InvestmentProduct product=products.findById(productId).orElseThrow(()->new FinancialException("Product not found"));
        if(!product.isActive())throw new FinancialException("This product is not currently active");
        Deposit payment=createZapUpi(user,product.getDiscountPrice(),returnOrigin);payment.setProductPurchase(productId,requestReference,clock.instant());return deposits.save(payment);
    }

    @Transactional
    public Deposit recordZapUpiOrder(String reference,String transactionId,String environment,String checkoutUrl,String payId,String paymentPayload){
        Deposit deposit=deposits.findByPaymentReferenceForUpdate(reference).orElseThrow(()->new FinancialException("Deposit not found"));
        if(deposit.getStatus()==Deposit.Status.PENDING){Instant now=clock.instant();deposit.markPending(transactionId,"ZAPUPI","Pending","ZapUPI order created in "+environment,null,now);deposit.setCheckout(checkoutUrl,payId,paymentPayload,now);}
        return deposits.save(deposit);
    }

    @Transactional
    public Deposit failZapUpiOrder(String reference,String transactionId,String message){
        Deposit deposit=deposits.findByPaymentReferenceForUpdate(reference).orElseThrow(()->new FinancialException("Deposit not found"));
        if(deposit.getStatus()==Deposit.Status.PENDING)deposit.markFailed(false,transactionId,"ZAPUPI","FAILED",message,null,clock.instant());
        return deposits.save(deposit);
    }

    @Transactional
    public Deposit createUpi(AppUser user,BigDecimal amount){
        limits.check("upi-create:"+user.getId(),10);validateAmount(amount);
        String reference="UPI"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase();
        return deposits.save(new Deposit(user,amount.setScale(2,RoundingMode.UNNECESSARY),reference,"UPI_QR",clock.instant()));
    }

    @Transactional
    public Deposit creditVerifiedUpi(String reference,String providerTransactionId,String bankReference){
        Deposit deposit=deposits.findByPaymentReferenceForUpdate(reference).orElseThrow(()->new FinancialException("Deposit not found"));
        if(deposit.getStatus()==Deposit.Status.SUCCESSFUL)return deposit;
        if(deposit.getStatus()!=Deposit.Status.PENDING)throw new FinancialException("A final deposit cannot be credited");
        Instant now=clock.instant();Wallet wallet=walletService.lock(deposit.getUser());BigDecimal before=wallet.getDepositBalance();wallet.creditDeposit(deposit.getAmount());
        if(deposit.markSuccessful(now,wallet.getDepositBalance())){deposit.updateGateway(providerTransactionId,"UPI","VERIFIED","Authenticated provider or bank-record verification",bankReference,now);wallets.save(wallet);if(!transactions.existsByReference(reference))transactions.save(WalletTransaction.upiDeposit(deposit.getUser(),deposit.getAmount(),before,wallet.getDepositBalance(),reference,now));}
        return deposits.save(deposit);
    }

    @Transactional
    public Deposit applyZapUpiVerification(ZapUpiClient.Verification verification,boolean liveMode) {
        Deposit deposit = deposits.findByPaymentReferenceForUpdate(verification.orderId())
                .orElseThrow(() -> new FinancialException("Deposit not found"));
        if (deposit.getStatus() == Deposit.Status.SUCCESSFUL) return deposit;
        if (verification.amount() != null && verification.amount().compareTo(deposit.getAmount()) != 0)
            throw new FinancialException("ZapUPI amount mismatch; payment was not credited");
        Instant now = clock.instant();
        if (verification.successful()) {
            if (verification.amount() == null || verification.paidAmount() == null || verification.paidAmount().compareTo(deposit.getAmount()) != 0)
                throw new FinancialException("ZapUPI did not confirm the exact paid amount");
            if (verification.transactionId()==null || verification.transactionId().isBlank() || verification.utr()==null || verification.utr().isBlank())
                throw new FinancialException("ZapUPI success lacked a transaction ID or UTR");
            if (liveMode && !"cashier".equalsIgnoreCase(verification.environment()))
                throw new FinancialException("A non-cashier ZapUPI payment cannot credit a live wallet");
            if("PRODUCT_PURCHASE".equals(deposit.getPaymentPurpose())){
                if(!investments.existsByUserIdAndProductId(deposit.getUser().getId(),deposit.getProductId())){
                    InvestmentProduct product=products.findById(deposit.getProductId()).orElseThrow(()->new FinancialException("Paid product no longer exists"));
                    UserInvestment investment=investments.saveAndFlush(new UserInvestment(deposit.getUser(),product,deposit.getPurchaseRequestReference(),now,rewardPeriodHours));
                    referrals.rewardFirstPurchase(deposit.getUser(),investment);
                }
                deposit.markSuccessful(now,null);deposit.updateGateway(verification.transactionId(),"ZAPUPI",verification.status(),verification.message(),verification.utr(),now);
            }else{
              Wallet wallet = walletService.lock(deposit.getUser());
              BigDecimal previousBalance = wallet.getDepositBalance();
              wallet.creditDeposit(deposit.getAmount());
              if (deposit.markSuccessful(now, wallet.getDepositBalance())) {
                deposit.updateGateway(verification.transactionId(), "ZAPUPI", verification.status(), verification.message(), verification.utr(), now);
                wallets.save(wallet);
                if (!transactions.existsByReference(deposit.getPaymentReference())) transactions.save(WalletTransaction.gatewayDeposit(deposit.getUser(),deposit.getAmount(),previousBalance,wallet.getDepositBalance(),deposit.getPaymentReference(),"ZAPUPI",now));
              }
            }
        } else if (verification.pending()) {
            deposit.markPending(verification.transactionId(), "ZAPUPI", verification.status(), verification.message(), verification.utr(), now);
        } else if (verification.paidAmount()!=null && verification.paidAmount().compareTo(deposit.getAmount())==0
                && verification.transactionId()!=null && !verification.transactionId().isBlank()
                && (verification.utr()==null || verification.utr().isBlank())) {
            // Cashier can temporarily report Failed after receiving the amount but before it has a
            // verifiable bank reference. Keep reconciling; never credit or invite another payment.
            deposit.markPending(verification.transactionId(), "ZAPUPI", "AWAITING_SETTLEMENT",
                    "ZapUPI received the amount but has not provided verified settlement confirmation", null, now);
        } else {
            deposit.markFailed(false, verification.transactionId(), "ZAPUPI", verification.status(), verification.message(), verification.utr(), now);
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

    @Transactional(readOnly = true)
    public Deposit findForUser(String reference, AppUser user) {
        Deposit deposit = deposits.findByPaymentReference(reference).orElseThrow(() -> new FinancialException("Deposit not found"));
        if (!deposit.getUser().getId().equals(user.getId())) throw new FinancialException("Deposit not found");
        return deposit;
    }

    @Transactional(readOnly = true)
    public Optional<Deposit> findSuccessfulForUser(String reference, AppUser user) {
        try { Deposit deposit=findForUser(reference,user);return deposit.getStatus()==Deposit.Status.SUCCESSFUL?Optional.of(deposit):Optional.empty(); }
        catch (FinancialException ignored) { return Optional.empty(); }
    }

    @Transactional(readOnly = true)
    public Optional<String> returnOrigin(String reference) {
        if (reference == null || reference.isBlank()) return Optional.empty();
        return deposits.findByPaymentReference(reference).map(Deposit::getReturnOrigin);
    }

    @Transactional(readOnly = true)
    public Optional<String> paymentPurpose(String reference){return deposits.findByPaymentReference(reference).map(Deposit::getPaymentPurpose);}

    public boolean isDemoEnabled() { return demoEnabled; }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0)
            throw new FinancialException("Deposit amount must be at least ₹1");
    }

    public static class FinancialException extends RuntimeException {
        public FinancialException(String message) { super(message); }
    }
}
