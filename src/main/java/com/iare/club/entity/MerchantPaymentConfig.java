package com.iare.club.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="merchant_payment_config",uniqueConstraints=@UniqueConstraint(name="uk_merchant_payment_singleton",columnNames="singleton_key"))
public class MerchantPaymentConfig{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @Column(name="singleton_key",nullable=false,length=20)private String singletonKey="PRIMARY";
 @Column(name="merchant_name",nullable=false,length=120)private String merchantName;
 @Column(name="upi_id",nullable=false,length=180)private String upiId;
 @Column(name="bank_name",nullable=false,length=100)private String bankName;
 @Column(name="account_last_four",length=4)private String accountLastFour;
 @Column(nullable=false)private boolean active;
 @Column(name="updated_at",nullable=false)private Instant updatedAt;
 @Column(name="updated_by",nullable=false,length=180)private String updatedBy;
 protected MerchantPaymentConfig(){}
 public MerchantPaymentConfig(String name,String upi,String bank,String lastFour,boolean active,String admin,Instant now){update(name,upi,bank,lastFour,active,admin,now);}
 public void update(String name,String upi,String bank,String lastFour,boolean active,String admin,Instant now){merchantName=name;upiId=upi;bankName=bank;accountLastFour=lastFour;this.active=active;updatedBy=admin;updatedAt=now;}
 public Long getId(){return id;}public String getMerchantName(){return merchantName;}public String getUpiId(){return upiId;}public String getBankName(){return bankName;}public String getAccountLastFour(){return accountLastFour;}public boolean isActive(){return active;}public Instant getUpdatedAt(){return updatedAt;}public String getUpdatedBy(){return updatedBy;}
 public String getMaskedAccount(){return accountLastFour==null||accountLastFour.isBlank()?"Not stored":"•••• "+accountLastFour;}
}
