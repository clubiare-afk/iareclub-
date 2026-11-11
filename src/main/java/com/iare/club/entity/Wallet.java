package com.iare.club.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="wallets",uniqueConstraints=@UniqueConstraint(name="uk_wallet_user",columnNames="user_id"))
public class Wallet {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @Column(name="deposit_balance",nullable=false,precision=18,scale=2) private BigDecimal depositBalance=BigDecimal.ZERO;
 @Column(name="reward_balance",nullable=false,precision=18,scale=2) private BigDecimal rewardBalance=BigDecimal.ZERO;
 @Column(name="total_invested",nullable=false,precision=18,scale=2) private BigDecimal totalInvested=BigDecimal.ZERO;
 @Column(name="total_rewards_earned",nullable=false,precision=18,scale=2) private BigDecimal totalRewardsEarned=BigDecimal.ZERO;
 @Column(name="total_withdrawn",nullable=false,precision=18,scale=2) private BigDecimal totalWithdrawn=BigDecimal.ZERO;
 @Column(name="pending_withdrawals",nullable=false,precision=18,scale=2) private BigDecimal pendingWithdrawals=BigDecimal.ZERO;
 @Version private long version;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Wallet(){} public Wallet(AppUser user){this.user=user;}
 @PrePersist @PreUpdate void touch(){updatedAt=Instant.now();}
 public Long getId(){return id;} public AppUser getUser(){return user;} public BigDecimal getDepositBalance(){return depositBalance;} public BigDecimal getRewardBalance(){return rewardBalance;} public BigDecimal getTotalInvested(){return totalInvested;} public BigDecimal getTotalRewardsEarned(){return totalRewardsEarned;} public BigDecimal getTotalWithdrawn(){return totalWithdrawn;} public BigDecimal getPendingWithdrawals(){return pendingWithdrawals;} public Instant getUpdatedAt(){return updatedAt;}
 public void creditDeposit(BigDecimal a){depositBalance=depositBalance.add(a);} public void debitDeposit(BigDecimal a){depositBalance=depositBalance.subtract(a);totalInvested=totalInvested.add(a);}
 public void creditReward(BigDecimal a){rewardBalance=rewardBalance.add(a);totalRewardsEarned=totalRewardsEarned.add(a);} public void reserveReward(BigDecimal a){rewardBalance=rewardBalance.subtract(a);pendingWithdrawals=pendingWithdrawals.add(a);}
 public void reverseReward(BigDecimal a){rewardBalance=rewardBalance.subtract(a);totalRewardsEarned=totalRewardsEarned.subtract(a);}
 public void completeWithdrawal(BigDecimal a){pendingWithdrawals=pendingWithdrawals.subtract(a);totalWithdrawn=totalWithdrawn.add(a);} public void refundWithdrawal(BigDecimal a){pendingWithdrawals=pendingWithdrawals.subtract(a);rewardBalance=rewardBalance.add(a);}
}
