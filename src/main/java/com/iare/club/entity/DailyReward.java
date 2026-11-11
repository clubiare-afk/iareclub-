package com.iare.club.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="daily_rewards",uniqueConstraints=@UniqueConstraint(name="uk_reward_investment_number",columnNames={"investment_id","reward_number"}),indexes=@Index(name="idx_reward_user_date",columnList="user_id,collected_at"))
public class DailyReward {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="investment_id",nullable=false) private UserInvestment investment;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @Column(name="reward_number",nullable=false) private int rewardNumber;
 @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
 @Column(name="collected_at",nullable=false,updatable=false) private Instant collectedAt;
 protected DailyReward(){} public DailyReward(UserInvestment i,AppUser u,int number,BigDecimal amount,Instant at){investment=i;user=u;rewardNumber=number;this.amount=amount;collectedAt=at;}
 public Long getId(){return id;} public int getRewardNumber(){return rewardNumber;} public BigDecimal getAmount(){return amount;} public Instant getCollectedAt(){return collectedAt;}
}
