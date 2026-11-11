package com.iare.club.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public class WithdrawalRequestDto{@NotNull @DecimalMin(value="1.00") @Digits(integer=12,fraction=2) private BigDecimal amount;public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal v){amount=v;}}
