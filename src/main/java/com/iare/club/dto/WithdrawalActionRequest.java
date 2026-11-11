package com.iare.club.dto;
import com.iare.club.entity.WithdrawalRequest;
import jakarta.validation.constraints.*;
public class WithdrawalActionRequest{@NotNull private WithdrawalRequest.Status status;@NotBlank @Size(max=500) private String remarks;@Size(max=120) private String payoutReference;public WithdrawalRequest.Status getStatus(){return status;}public void setStatus(WithdrawalRequest.Status v){status=v;}public String getRemarks(){return remarks;}public void setRemarks(String v){remarks=v;}public String getPayoutReference(){return payoutReference;}public void setPayoutReference(String v){payoutReference=v;}}
