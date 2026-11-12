package com.iare.club.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="support_status_history",indexes=@Index(name="idx_support_status_ticket_created",columnList="ticket_id,created_at"))
public class SupportStatusHistory{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="ticket_id",nullable=false)private SupportTicket ticket;
 @Enumerated(EnumType.STRING)@Column(name="previous_status",length=20)private SupportTicket.Status previousStatus;
 @Enumerated(EnumType.STRING)@Column(name="new_status",nullable=false,length=20)private SupportTicket.Status newStatus;
 @Column(name="changed_by",nullable=false,length=180)private String changedBy;
 @Column(name="created_at",nullable=false,updatable=false)private Instant createdAt;
 @PrePersist void create(){createdAt=Instant.now();}
 public Long getId(){return id;}public void setTicket(SupportTicket v){ticket=v;}public SupportTicket.Status getPreviousStatus(){return previousStatus;}public void setPreviousStatus(SupportTicket.Status v){previousStatus=v;}public SupportTicket.Status getNewStatus(){return newStatus;}public void setNewStatus(SupportTicket.Status v){newStatus=v;}public String getChangedBy(){return changedBy;}public void setChangedBy(String v){changedBy=v;}public Instant getCreatedAt(){return createdAt;}
}
