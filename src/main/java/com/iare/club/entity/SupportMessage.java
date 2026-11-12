package com.iare.club.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="support_messages",indexes=@Index(name="idx_support_message_ticket_created",columnList="ticket_id,created_at"))
public class SupportMessage{
 public enum Sender{USER,ADMIN}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="ticket_id",nullable=false)private SupportTicket ticket;
 @Enumerated(EnumType.STRING)@Column(name="sender_type",nullable=false,length=10)private Sender sender;
 @Column(name="sender_name",nullable=false,length=180)private String senderName;
 @Column(nullable=false,columnDefinition="text")private String message;
 @Column(name="created_at",nullable=false,updatable=false)private Instant createdAt;
 @PrePersist void create(){createdAt=Instant.now();}
 public Long getId(){return id;}public SupportTicket getTicket(){return ticket;}public void setTicket(SupportTicket v){ticket=v;}public Sender getSender(){return sender;}public void setSender(Sender v){sender=v;}public String getSenderName(){return senderName;}public void setSenderName(String v){senderName=v;}public String getMessage(){return message;}public void setMessage(String v){message=v;}public Instant getCreatedAt(){return createdAt;}
}
