package com.iare.club.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="user_investments",indexes={@Index(name="idx_investment_user_status",columnList="user_id,status")},uniqueConstraints={@UniqueConstraint(name="uk_investment_request",columnNames="request_reference"),@UniqueConstraint(name="uk_investment_user_product",columnNames={"user_id","product_id"})})
public class UserInvestment {
 public enum Status{ACTIVE,COMPLETED}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="product_id",nullable=false) private InvestmentProduct product;
 @Column(name="product_name",nullable=false,length=120) private String productName;
 @Column(name="product_image_url",nullable=false,length=1000) private String productImageUrl;
 @Column(name="investment_amount",nullable=false,precision=18,scale=2) private BigDecimal investmentAmount;
 @Column(name="daily_reward",nullable=false,precision=18,scale=2) private BigDecimal dailyReward;
 @Column(name="total_days",nullable=false) private int totalDays;
 @Column(name="projected_returns",nullable=false,precision=18,scale=2) private BigDecimal projectedReturns;
 @Column(name="rewards_collected",nullable=false) private int rewardsCollected;
 @Column(name="total_rewards_collected",nullable=false,precision=18,scale=2) private BigDecimal totalRewardsCollected=BigDecimal.ZERO;
 @Column(name="next_reward_at",nullable=false) private Instant nextRewardAt;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.ACTIVE;
 @Column(name="request_reference",nullable=false,length=100) private String requestReference;
 @Column(name="purchased_at",nullable=false,updatable=false) private Instant purchasedAt;
 @Version private long version;
 protected UserInvestment(){}
 public UserInvestment(AppUser u,InvestmentProduct p,String request,Instant now,int periodHours){user=u;product=p;productName=p.getName();productImageUrl=p.getImageUrl();investmentAmount=p.getDiscountPrice();dailyReward=p.getDailyEarnings();totalDays=p.getTotalDays();projectedReturns=p.getTotalReturns();requestReference=request;purchasedAt=now;nextRewardAt=now;}
 public void collect(Instant next){rewardsCollected++;totalRewardsCollected=totalRewardsCollected.add(dailyReward);if(rewardsCollected>=totalDays)status=Status.COMPLETED;else nextRewardAt=next;}
 public Long getId(){return id;} public AppUser getUser(){return user;} public InvestmentProduct getProduct(){return product;} public String getProductName(){return productName;} public String getProductImageUrl(){return productImageUrl;} public BigDecimal getInvestmentAmount(){return investmentAmount;} public BigDecimal getDailyReward(){return dailyReward;} public int getTotalDays(){return totalDays;} public BigDecimal getProjectedReturns(){return projectedReturns;} public int getRewardsCollected(){return rewardsCollected;} public int getRemainingDays(){return Math.max(0,totalDays-rewardsCollected);} public BigDecimal getTotalRewardsCollected(){return totalRewardsCollected;} public Instant getNextRewardAt(){return nextRewardAt;} public Status getStatus(){return status;} public String getRequestReference(){return requestReference;} public Instant getPurchasedAt(){return purchasedAt;}
 @Transient public int getProgressPercentage(){return totalDays==0?0:Math.min(100,rewardsCollected*100/totalDays);}
 @Transient public BigDecimal getProjectedRemainingRewards(){return dailyReward.multiply(BigDecimal.valueOf(getRemainingDays()));}
 @Transient public BigDecimal getProjectedNetProfit(){return projectedReturns.subtract(investmentAmount);}
}
