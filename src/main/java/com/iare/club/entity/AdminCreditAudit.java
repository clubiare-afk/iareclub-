package com.iare.club.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "admin_credit_audits", indexes = @Index(name = "idx_admin_credit_audit_user_date", columnList = "user_id,created_at"))
public class AdminCreditAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 100) private String transactionReference;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "payment_method", nullable = false, length = 30) private String paymentMethod;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "balance_before", nullable = false, precision = 18, scale = 2) private BigDecimal balanceBefore;
    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAfter;
    @Column(name = "admin_identity", nullable = false, length = 180) private String adminIdentity;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected AdminCreditAudit() {}
    public AdminCreditAudit(Long userId, String reference, BigDecimal amount, BigDecimal before,
                            BigDecimal after, String adminIdentity, Instant createdAt) {
        this.userId = userId; this.transactionReference = reference; this.amount = amount;
        this.paymentMethod = "ADMIN_CREDIT"; this.status = "SUCCESSFUL";
        this.balanceBefore = before; this.balanceAfter = after;
        this.adminIdentity = adminIdentity; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTransactionReference() { return transactionReference; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getAdminIdentity() { return adminIdentity; }
    public Instant getCreatedAt() { return createdAt; }
}
