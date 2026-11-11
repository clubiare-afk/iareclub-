package com.iare.club.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="admin_audit_logs",indexes=@Index(name="idx_audit_entity",columnList="entity_type,entity_id"))
public class AdminAuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="admin_identity",nullable=false,length=180) private String adminIdentity;
 @Column(name="entity_type",nullable=false,length=50) private String entityType;
 @Column(name="entity_id",nullable=false) private Long entityId;
 @Column(nullable=false,length=80) private String action;
 @Column(name="previous_status",length=30) private String previousStatus;
 @Column(name="new_status",length=30) private String newStatus;
 @Column(length=500) private String remarks;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected AdminAuditLog(){} public AdminAuditLog(String admin,String entityType,Long entityId,String action,String previous,String next,String remarks,Instant at){adminIdentity=admin;this.entityType=entityType;this.entityId=entityId;this.action=action;previousStatus=previous;newStatus=next;this.remarks=remarks;createdAt=at;}
 public Long getId(){return id;} public String getAdminIdentity(){return adminIdentity;} public String getAction(){return action;} public String getPreviousStatus(){return previousStatus;} public String getNewStatus(){return newStatus;} public String getRemarks(){return remarks;} public Instant getCreatedAt(){return createdAt;}
}
