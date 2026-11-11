package com.iare.club.service;

import com.iare.club.entity.*;
import com.iare.club.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralService {
    private final ReferralRepository referrals; private final UserRepository users; private final WalletRepository wallets;
    private final WalletTransactionRepository transactions; private final WalletService walletService; private final Clock clock;
    private final BigDecimal bonus; private final String publicBaseUrl;
    public ReferralService(ReferralRepository referrals,UserRepository users,WalletRepository wallets,WalletTransactionRepository transactions,
            WalletService walletService,Clock clock,@Value("${moneymint.referral.bonus:200.00}")BigDecimal bonus,
            @Value("${moneymint.public-base-url:http://localhost:8080}")String publicBaseUrl){
        this.referrals=referrals;this.users=users;this.wallets=wallets;this.transactions=transactions;this.walletService=walletService;this.clock=clock;
        this.bonus=bonus.setScale(2);this.publicBaseUrl=publicBaseUrl.replaceAll("/$","");
    }

    @Transactional
    public String ensureCode(AppUser user){
        AppUser locked=users.findByIdForUpdate(user.getId()).orElseThrow();
        if(locked.getReferralCode()!=null&&!locked.getReferralCode().isBlank())return locked.getReferralCode();
        for(int i=0;i<10;i++){
            String code="MM"+Long.toString(locked.getId(),36).toUpperCase(Locale.ROOT)+UUID.randomUUID().toString().replace("-","").substring(0,6).toUpperCase(Locale.ROOT);
            if(!users.existsByReferralCode(code)){locked.setReferralCode(code);users.saveAndFlush(locked);user.setReferralCode(code);return code;}
        }
        throw new IllegalStateException("Unable to generate a referral code");
    }

    @Transactional
    public void recordSignup(AppUser referredUser,String suppliedCode){
        if(suppliedCode==null||suppliedCode.isBlank())return;
        String code=suppliedCode.strip().toUpperCase(Locale.ROOT);
        AppUser referrer=users.findByReferralCode(code).orElseThrow(InvalidReferralCodeException::new);
        if(referrer.getId().equals(referredUser.getId())||referrer.getEmail().equalsIgnoreCase(referredUser.getEmail()))throw new SelfReferralException();
        if(referrals.findByReferredUserId(referredUser.getId()).isPresent())return;
        referrals.save(new Referral(referrer,referredUser,code,clock.instant()));
    }

    @Transactional
    public void rewardFirstPurchase(AppUser referredUser,UserInvestment investment){
        Referral referral=referrals.findByReferredUserIdForUpdate(referredUser.getId()).orElse(null);
        if(referral==null||referral.getStatus()!=Referral.Status.PENDING)return;
        Wallet wallet=walletService.lock(referral.getReferrer());
        String reference="REF-BONUS-"+referral.getId();
        if(transactions.existsByReference(reference))return;
        referral.qualify(investment,bonus,clock.instant());
        wallet.creditReward(bonus);
        wallets.save(wallet);referrals.save(referral);
        transactions.save(new WalletTransaction(referral.getReferrer(),WalletTransaction.Type.REFERRAL_BONUS,WalletTransaction.Status.SUCCESSFUL,
            bonus,wallet.getRewardBalance(),reference,"Referral bonus for "+referredUser.getFullName()+"'s first confirmed purchase",clock.instant()));
    }

    @Transactional(readOnly=true)
    public Dashboard dashboard(AppUser user){
        String code=user.getReferralCode();
        return new Dashboard(referrals.countByReferrerId(user.getId()),referrals.countByReferrerIdAndStatus(user.getId(),Referral.Status.PENDING),
            referrals.countByReferrerIdAndStatus(user.getId(),Referral.Status.SUCCESSFUL),referrals.totalSuccessfulFor(user.getId()),
            publicBaseUrl+"/signup?ref="+code,referrals.findTop100ByReferrerIdOrderByCreatedAtDesc(user.getId()));
    }

    @Transactional(readOnly=true) public AdminDashboard adminDashboard(int page){
        return new AdminDashboard(referrals.count(),referrals.countByStatus(Referral.Status.PENDING),referrals.countByStatus(Referral.Status.SUCCESSFUL),
            referrals.countByStatus(Referral.Status.REVERSED),referrals.totalExpense(),referrals.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page,0),25)));
    }

    @Transactional
    public void reverse(long referralId,String reason){
        if(reason==null||reason.isBlank())throw new ReferralException("A reversal reason is required");
        Referral referral=referrals.findById(referralId).orElseThrow(()->new ReferralException("Referral not found"));
        if(referral.getStatus()==Referral.Status.REVERSED)return;
        if(referral.getStatus()!=Referral.Status.SUCCESSFUL)throw new ReferralException("Only a paid bonus can be reversed");
        Wallet wallet=walletService.lock(referral.getReferrer());
        if(wallet.getRewardBalance().compareTo(referral.getBonusAmount())<0)throw new ReferralException("The available withdrawal balance is too low to reverse this bonus safely");
        String reference="REF-REVERSAL-"+referral.getId();
        if(transactions.existsByReference(reference))return;
        wallet.reverseReward(referral.getBonusAmount());referral.reverse(reason.strip(),clock.instant());
        wallets.save(wallet);referrals.save(referral);
        transactions.save(new WalletTransaction(referral.getReferrer(),WalletTransaction.Type.REFERRAL_REVERSAL,WalletTransaction.Status.REFUNDED,
            referral.getBonusAmount().negate(),wallet.getRewardBalance(),reference,"Referral bonus reversed: "+reason.strip(),clock.instant()));
    }

    public BigDecimal getBonus(){return bonus;}
    public record Dashboard(long total,long pending,long successful,BigDecimal earnings,String link,List<Referral> history){}
    public record AdminDashboard(long total,long pending,long successful,long reversed,BigDecimal expense,Page<Referral> referrals){}
    public static class ReferralException extends RuntimeException{public ReferralException(String message){super(message);}}
    public static class InvalidReferralCodeException extends ReferralException{public InvalidReferralCodeException(){super("This referral code is not valid");}}
    public static class SelfReferralException extends ReferralException{public SelfReferralException(){super("You cannot refer yourself");}}
}
