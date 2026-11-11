package com.iare.club.repository;

import com.iare.club.entity.AdminCreditAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCreditAuditRepository extends JpaRepository<AdminCreditAudit, Long> {
    boolean existsByTransactionReference(String transactionReference);
}
