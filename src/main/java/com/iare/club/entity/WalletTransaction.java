package com.iare.club.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="wallet_transactions",indexes={@Index(name="idx_wallet_tx_user_date",columnList="user_id,created_at")},uniqueConstraints=@UniqueConstraint(name="uk_wallet_tx_reference",columnNames="reference"))
public class WalletTransaction {
 public enum Type{DEPOSIT,ADMIN_CREDIT,INVESTMENT_PURCHASE,DAILY_REWARD,SCRATCH_REWARD,REFERRAL_BONUS,REFERRAL_REVERSAL,WITHDRAWAL_REQUEST,WITHDRAWAL_SUCCESS,WITHDRAWAL_REFUND}
 public enum Status{PENDING,SUCCESSFUL,FAILED,PROCESSING,REFUNDED}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private Type type;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
 @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
 @Column(name="balance_after",precision=18,scale=2) private BigDecimal balanceAfter;
 @Column(name="balance_before",precision=18,scale=2) private BigDecimal balanceBefore;
 @Column(name="payment_method",length=30) private String paymentMethod;
 @Column(name="admin_identity",length=180) private String adminIdentity;
 @Column(nullable=false,length=100) private String reference;
 @Column(nullable=false,length=250) private String description;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected WalletTransaction(){}
 public WalletTransaction(AppUser u,Type t,Status s,BigDecimal a,BigDecimal after,String ref,String description,Instant at){user=u;type=t;status=s;amount=a;balanceAfter=after;reference=ref;this.description=description;createdAt=at;}
 public static WalletTransaction adminCredit(AppUser user,BigDecimal amount,BigDecimal before,BigDecimal after,String reference,String admin,Instant at){WalletTransaction transaction=new WalletTransaction(user,Type.ADMIN_CREDIT,Status.SUCCESSFUL,amount,after,reference,"Wallet credited by an authorized Money Mint administrator",at);transaction.balanceBefore=before;transaction.paymentMethod="ADMIN_CREDIT";transaction.adminIdentity=admin;return transaction;}
 public Long getId(){return id;} public AppUser getUser(){return user;} public Type getType(){return type;} public Status getStatus(){return status;} public BigDecimal getAmount(){return amount;} public BigDecimal getBalanceAfter(){return balanceAfter;} public BigDecimal getBalanceBefore(){return balanceBefore;} public String getPaymentMethod(){return paymentMethod;} public String getAdminIdentity(){return adminIdentity;} public String getReference(){return reference;} public String getDescription(){return description;} public Instant getCreatedAt(){return createdAt;}
}
