package com.iare.club.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="bank_accounts",uniqueConstraints=@UniqueConstraint(name="uk_bank_user",columnNames="user_id"))
public class BankAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @Column(name="account_holder_name",nullable=false,length=100) private String accountHolderName;
 @Column(name="bank_name",nullable=false,length=100) private String bankName;
 @Column(name="account_number_encrypted",nullable=false,length=600) private String accountNumberEncrypted;
 @Column(name="account_last_four",nullable=false,length=4) private String accountLastFour;
 @Column(name="ifsc_encrypted",nullable=false,length=300) private String ifscEncrypted;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected BankAccount(){} public BankAccount(AppUser u){user=u;}
 @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=Instant.now();}
 public void updateDetails(String holder,String bank,String encryptedNumber,String last4,String encryptedIfsc){accountHolderName=holder;bankName=bank;accountNumberEncrypted=encryptedNumber;accountLastFour=last4;ifscEncrypted=encryptedIfsc;}
 public Long getId(){return id;} public AppUser getUser(){return user;} public String getAccountHolderName(){return accountHolderName;} public String getBankName(){return bankName;} public String getAccountNumberEncrypted(){return accountNumberEncrypted;} public String getAccountLastFour(){return accountLastFour;} public String getIfscEncrypted(){return ifscEncrypted;} public String getMaskedAccountNumber(){return "•••• •••• "+accountLastFour;} public Instant getUpdatedAt(){return updatedAt;}
}
