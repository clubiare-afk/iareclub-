package com.iare.club.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

@Entity
@Table(name="deposits", indexes={
    @Index(name="idx_deposit_user_date",columnList="user_id,created_at"),
    @Index(name="idx_deposit_status",columnList="status")
}, uniqueConstraints=@UniqueConstraint(name="uk_deposit_payment_reference",columnNames="payment_reference"))
public class Deposit {
    public enum Status { PENDING, SUCCESSFUL, FAILED, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
    @Column(name="payment_reference",nullable=false,length=100) private String paymentReference;
    @Column(name="payu_payment_id",length=100) private String payuPaymentId;
    @Column(name="bank_reference",length=150) private String bankReference;
    @Column(name="payment_method",nullable=false,length=30) private String paymentMethod;
    @Column(name="gateway_status",length=40) private String gatewayStatus;
    @Column(name="gateway_message",length=500) private String gatewayMessage;
    @Column(name="wallet_balance_after",precision=18,scale=2) private BigDecimal walletBalanceAfter;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Column(name="completed_at") private Instant completedAt;
    @Version private long version;

    protected Deposit() {}
    public Deposit(AppUser user,BigDecimal amount,String reference,String method,Instant at){this.user=user;this.amount=amount;paymentReference=reference;paymentMethod=method;status=Status.PENDING;createdAt=at;updatedAt=at;}
    public boolean markSuccessful(Instant at,BigDecimal balance){if(status==Status.SUCCESSFUL)return false;if(status!=Status.PENDING)throw new IllegalStateException("A final payment cannot be credited");status=Status.SUCCESSFUL;completedAt=at;updatedAt=at;walletBalanceAfter=balance;return true;}
    public void markPending(String payuId,String method,String gateway,String message,String bankRef,Instant at){if(status!=Status.PENDING)return;updateGateway(payuId,method,gateway,message,bankRef,at);}
    public void markFailed(boolean cancelled,String payuId,String method,String gateway,String message,String bankRef,Instant at){if(status!=Status.PENDING)return;status=cancelled?Status.CANCELLED:Status.FAILED;completedAt=at;updateGateway(payuId,method,gateway,message,bankRef,at);}
    public void updateGateway(String payuId,String method,String gateway,String message,String bankRef,Instant at){payuPaymentId=clean(payuId,100);if(method!=null&&!method.isBlank())paymentMethod=clean(method,30);gatewayStatus=clean(gateway,40);gatewayMessage=clean(message,500);bankReference=clean(bankRef,150);updatedAt=at;}
    private static String clean(String value,int max){return value==null?null:value.substring(0,Math.min(value.length(),max));}
    @Transient
    public String getDisplayPaymentId(){
        if(!"ADMIN_CREDIT".equals(paymentMethod))return payuPaymentId==null?"—":payuPaymentId;
        CRC32 checksum=new CRC32();checksum.update(paymentReference.getBytes(StandardCharsets.UTF_8));
        return String.format("%010d",checksum.getValue());
    }
    @Transient
    public String getDisplayMethod(){return "ADMIN_CREDIT".equals(paymentMethod)?"WALLET":paymentMethod;}
    @Transient
    public String getDisplayProvider(){return "ADMIN_CREDIT".equals(paymentMethod)?"Money Mint":"PayU";}
    public Long getId(){return id;} public AppUser getUser(){return user;} public BigDecimal getAmount(){return amount;} public String getPaymentReference(){return paymentReference;} public String getPayuPaymentId(){return payuPaymentId;} public String getBankReference(){return bankReference;} public String getPaymentMethod(){return paymentMethod;} public String getGatewayStatus(){return gatewayStatus;} public String getGatewayMessage(){return gatewayMessage;} public BigDecimal getWalletBalanceAfter(){return walletBalanceAfter;} public Status getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Instant getCompletedAt(){return completedAt;}
}
