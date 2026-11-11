package com.iare.club.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="withdrawal_requests",indexes={@Index(name="idx_withdrawal_user_date",columnList="user_id,requested_at"),@Index(name="idx_withdrawal_status",columnList="status")},uniqueConstraints=@UniqueConstraint(name="uk_withdrawal_reference",columnNames="reference"))
public class WithdrawalRequest {
 public enum Status{PENDING,PROCESSING,SUCCESSFUL,FAILED,REFUNDED}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="bank_account_id",nullable=false) private BankAccount bankAccount;
 @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=100) private String reference;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.PENDING;
 @Column(name="requested_at",nullable=false,updatable=false) private Instant requestedAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @Column(name="payout_reference",length=120) private String payoutReference;
 @Column(name="admin_remarks",length=500) private String adminRemarks;
 @Column(name="funds_released",nullable=false) private boolean fundsReleased;
 @Version private long version;
 protected WithdrawalRequest(){} public WithdrawalRequest(AppUser u,BankAccount bank,BigDecimal amount,String ref,Instant now){user=u;bankAccount=bank;this.amount=amount;reference=ref;requestedAt=now;updatedAt=now;}
 public void transition(Status next,String remarks,String payout,Instant at){status=next;adminRemarks=remarks;payoutReference=payout;updatedAt=at;} public void markFundsReleased(){fundsReleased=true;}
 public Long getId(){return id;} public AppUser getUser(){return user;} public BankAccount getBankAccount(){return bankAccount;} public BigDecimal getAmount(){return amount;} public String getReference(){return reference;} public Status getStatus(){return status;} public Instant getRequestedAt(){return requestedAt;} public Instant getUpdatedAt(){return updatedAt;} public String getPayoutReference(){return payoutReference;} public String getAdminRemarks(){return adminRemarks;} public boolean isFundsReleased(){return fundsReleased;}
}
