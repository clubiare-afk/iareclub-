package com.iare.club.dto;
import jakarta.validation.constraints.*;
public class BankAccountRequest{
 @NotBlank @Size(max=100) private String accountHolderName; @NotBlank @Size(max=100) private String bankName;
 @NotBlank @Pattern(regexp="^[0-9]{6,20}$",message="Account number must contain 6 to 20 digits") private String accountNumber;
 @NotBlank private String confirmAccountNumber;
 @NotBlank @Pattern(regexp="^[A-Z]{4}0[A-Z0-9]{6}$",message="Enter a valid IFSC code") private String ifscCode;
 @AssertTrue(message="Account numbers do not match") public boolean isAccountNumberConfirmed(){return accountNumber!=null&&accountNumber.equals(confirmAccountNumber);}
 public String getAccountHolderName(){return accountHolderName;}public void setAccountHolderName(String v){accountHolderName=v;}public String getBankName(){return bankName;}public void setBankName(String v){bankName=v;}public String getAccountNumber(){return accountNumber;}public void setAccountNumber(String v){accountNumber=v;}public String getConfirmAccountNumber(){return confirmAccountNumber;}public void setConfirmAccountNumber(String v){confirmAccountNumber=v;}public String getIfscCode(){return ifscCode;}public void setIfscCode(String v){ifscCode=v;}
}
