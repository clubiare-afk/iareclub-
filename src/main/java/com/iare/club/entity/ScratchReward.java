package com.iare.club.entity;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="scratch_rewards",indexes=@Index(name="idx_scratch_user_date",columnList="user_id,scratched_at"))
public class ScratchReward{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @Column(name="game_day",nullable=false) private LocalDate gameDay;
 @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
 @Column(name="transaction_reference",nullable=false,unique=true,length=100) private String transactionReference;
 @Column(name="scratched_at",nullable=false,updatable=false) private Instant scratchedAt;
 @Column(nullable=false,length=20) private String status="SUCCESSFUL";
 protected ScratchReward(){}
 public ScratchReward(AppUser user,LocalDate day,BigDecimal amount,String reference,Instant at){this.user=user;gameDay=day;this.amount=amount;transactionReference=reference;scratchedAt=at;}
 public Long getId(){return id;}public AppUser getUser(){return user;}public LocalDate getGameDay(){return gameDay;}public BigDecimal getAmount(){return amount;}public String getTransactionReference(){return transactionReference;}public Instant getScratchedAt(){return scratchedAt;}public String getStatus(){return status;}
}
