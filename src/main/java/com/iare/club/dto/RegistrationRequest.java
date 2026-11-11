package com.iare.club.dto;
import jakarta.validation.constraints.*;
public class RegistrationRequest {
    @NotBlank(message="Full name is required") @Size(min=2,max=100,message="Full name must be between 2 and 100 characters")
    @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$",message="Enter a valid full name") private String fullName;
    @NotBlank(message="Email is required") @Email(message="Enter a valid email address") @Size(max=180,message="Email is too long") private String email;
    @NotBlank(message="Password is required") @Size(min=8,max=72,message="Password must be 8 to 72 characters")
    @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",message="Use uppercase, lowercase, and a number") private String password;
    @Size(max=16,message="Referral code is invalid") @Pattern(regexp="^[A-Za-z0-9]*$",message="Referral code is invalid") private String referralCode;
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getReferralCode(){return referralCode;} public void setReferralCode(String v){referralCode=v;}
}
