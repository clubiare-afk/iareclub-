package com.iare.club.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public class DepositRequest {
    @NotNull @DecimalMin(value="1.00",message="Deposit amount must be at least ₹1") @Digits(integer=12,fraction=2) private BigDecimal amount;
    private String phone;
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal amount){this.amount=amount;}
    public String getPhone(){return phone;} public void setPhone(String phone){this.phone=phone;}
}
