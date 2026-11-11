package com.iare.club.repository;
import com.iare.club.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog,Long>{}
