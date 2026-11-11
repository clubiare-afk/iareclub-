package com.iare.club.repository;

import java.math.BigDecimal;

public interface AdminWalletUserView {
    Long getUserId();
    String getFullName();
    String getEmail();
    BigDecimal getDepositBalance();
    BigDecimal getRewardBalance();
    BigDecimal getCurrentBalance();
    BigDecimal getTotalDeposits();
    BigDecimal getTotalWithdrawals();
}
