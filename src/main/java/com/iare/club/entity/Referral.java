package com.iare.club.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="referrals", indexes={
    @Index(name="idx_referral_referrer_status",columnList="referrer_id,status"),
    @Index(name="idx_referral_created_at",columnList="created_at")
}, uniqueConstraints={
    @UniqueConstraint(name="uk_referral_referred_user",columnNames="referred_user_id"),
    @UniqueConstraint(name="uk_referral_qualifying_investment",columnNames="qualifying_investment_id")
})
public class Referral {
    public enum Status { PENDING, SUCCESSFUL, REVERSED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="referrer_id",nullable=false) private AppUser referrer;
    @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="referred_user_id",nullable=false) private AppUser referredUser;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="qualifying_investment_id") private UserInvestment qualifyingInvestment;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.PENDING;
    @Column(name="bonus_amount",nullable=false,precision=18,scale=2) private BigDecimal bonusAmount=BigDecimal.ZERO;
    @Column(name="referral_code_used",nullable=false,length=16) private String referralCodeUsed;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="qualified_at") private Instant qualifiedAt;
    @Column(name="reversed_at") private Instant reversedAt;
    @Column(name="reversal_reason",length=300) private String reversalReason;
    @Version private long version;

    protected Referral() {}
    public Referral(AppUser referrer,AppUser referredUser,String code,Instant now){this.referrer=referrer;this.referredUser=referredUser;referralCodeUsed=code;createdAt=now;}
    public void qualify(UserInvestment investment,BigDecimal amount,Instant now){if(status!=Status.PENDING)return;qualifyingInvestment=investment;bonusAmount=amount;status=Status.SUCCESSFUL;qualifiedAt=now;}
    public void reverse(String reason,Instant now){if(status!=Status.SUCCESSFUL)throw new IllegalStateException("Only a successful referral can be reversed");status=Status.REVERSED;reversedAt=now;reversalReason=reason;}
    public Long getId(){return id;} public AppUser getReferrer(){return referrer;} public AppUser getReferredUser(){return referredUser;} public UserInvestment getQualifyingInvestment(){return qualifyingInvestment;} public Status getStatus(){return status;} public BigDecimal getBonusAmount(){return bonusAmount;} public String getReferralCodeUsed(){return referralCodeUsed;} public Instant getCreatedAt(){return createdAt;} public Instant getQualifiedAt(){return qualifiedAt;} public Instant getReversedAt(){return reversedAt;} public String getReversalReason(){return reversalReason;}
}
