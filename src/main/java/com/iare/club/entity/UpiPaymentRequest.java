package com.iare.club.entity;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.Instant;
@Entity @Table(name="upi_payment_requests",indexes={@Index(name="idx_upi_payment_user_date",columnList="user_id,created_at"),@Index(name="idx_upi_payment_status_date",columnList="status,created_at")})
public class UpiPaymentRequest{
 public enum Status{CREATED,PENDING,DETECTED_UNVERIFIED,VERIFIED,COMPLETED,FAILED,EXPIRED,REQUIRES_REVIEW}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY)@JoinColumn(name="user_id")private AppUser user;
 @OneToOne(optional=false,fetch=FetchType.LAZY)@JoinColumn(name="deposit_id",unique=true)private Deposit deposit;
 @Column(nullable=false,unique=true,length=100)private String reference;
 @Column(nullable=false,precision=18,scale=2)private BigDecimal amount;
 @Column(name="merchant_upi_id",nullable=false,length=180)private String merchantUpiId;
 @Column(name="merchant_name",nullable=false,length=120)private String merchantName;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=30)private Status status=Status.PENDING;
 @Column(length=40)private String provider;@Column(name="provider_transaction_id",length=150)private String providerTransactionId;
 @Column(name="youpi_payment_id",length=100)private String youpiPaymentId;@Column(name="checkout_url",length=1000)private String checkoutUrl;@Column(name="gateway_status",length=40)private String gatewayStatus;
 @Column(name="bank_reference",length=150)private String bankReference;@Column(name="verification_evidence",length=500)private String verificationEvidence;
 @Column(name="detected_at")private Instant detectedAt;@Column(name="verified_at")private Instant verifiedAt;@Column(name="wallet_credited_at")private Instant walletCreditedAt;
 @Column(name="expires_at",nullable=false)private Instant expiresAt;@Column(name="created_at",nullable=false)private Instant createdAt;@Column(name="updated_at",nullable=false)private Instant updatedAt;@Version private long version;
 protected UpiPaymentRequest(){}
 public UpiPaymentRequest(AppUser user,Deposit deposit,String reference,BigDecimal amount,String upi,String name,Instant now,Instant expires){this.user=user;this.deposit=deposit;this.reference=reference;this.amount=amount;merchantUpiId=upi;merchantName=name;status=Status.CREATED;createdAt=now;updatedAt=now;expiresAt=expires;}
 public void bindYoupi(String paymentId,String checkoutUrl,String gatewayStatus,Instant now){youpiPaymentId=clean(paymentId,100);this.checkoutUrl=clean(checkoutUrl,1000);this.gatewayStatus=clean(gatewayStatus,40);status=Status.PENDING;updatedAt=now;}
 public void markPending(Instant now){if(status==Status.CREATED){status=Status.PENDING;updatedAt=now;}}
 public void observed(String provider,String transaction,String utr,String evidence,Instant now){if(status==Status.VERIFIED||status==Status.FAILED)return;status=Status.DETECTED_UNVERIFIED;this.provider=clean(provider,40);providerTransactionId=clean(transaction,150);bankReference=clean(utr,150);verificationEvidence=clean(evidence,500);detectedAt=now;updatedAt=now;}
 public void requireReview(String provider,String transaction,String utr,String evidence,Instant now){if(status==Status.VERIFIED)return;status=Status.REQUIRES_REVIEW;this.provider=clean(provider,40);providerTransactionId=clean(transaction,150);bankReference=clean(utr,150);verificationEvidence=clean(evidence,500);updatedAt=now;}
 public void gatewayFailed(String outcome,String detail,Instant now){if(status==Status.COMPLETED||status==Status.VERIFIED)return;gatewayStatus=clean(outcome,40);verificationEvidence=clean(detail,500);status="EXPIRED".equalsIgnoreCase(outcome)?Status.EXPIRED:Status.FAILED;updatedAt=now;}
 public void verified(String provider,String transaction,String utr,String evidence,Instant now){if(status==Status.VERIFIED)return;status=Status.VERIFIED;this.provider=clean(provider,40);providerTransactionId=clean(transaction,150);bankReference=clean(utr,150);verificationEvidence=clean(evidence,500);verifiedAt=now;updatedAt=now;}
 public void credited(Instant now){walletCreditedAt=now;status=Status.COMPLETED;updatedAt=now;}public void expire(Instant now){if(status==Status.CREATED||status==Status.PENDING){status=Status.EXPIRED;updatedAt=now;}}
 private static String clean(String s,int max){return s==null?null:s.strip().substring(0,Math.min(s.strip().length(),max));}
 public Long getId(){return id;}public AppUser getUser(){return user;}public Deposit getDeposit(){return deposit;}public String getReference(){return reference;}public BigDecimal getAmount(){return amount;}public String getMerchantUpiId(){return merchantUpiId;}public String getMerchantName(){return merchantName;}public Status getStatus(){return status;}public String getProvider(){return provider;}public String getProviderTransactionId(){return providerTransactionId;}public String getYoupiPaymentId(){return youpiPaymentId;}public String getCheckoutUrl(){return checkoutUrl;}public String getGatewayStatus(){return gatewayStatus;}public String getBankReference(){return bankReference;}public String getVerificationEvidence(){return verificationEvidence;}public Instant getDetectedAt(){return detectedAt;}public Instant getVerifiedAt(){return verifiedAt;}public Instant getWalletCreditedAt(){return walletCreditedAt;}public Instant getExpiresAt(){return expiresAt;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
}
