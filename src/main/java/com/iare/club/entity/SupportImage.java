package com.iare.club.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="support_images",indexes=@Index(name="idx_support_image_ticket",columnList="ticket_id"))
public class SupportImage{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="ticket_id",nullable=false)private SupportTicket ticket;
 @Column(name="secure_url",nullable=false,length=1000)private String secureUrl;
 @Column(name="public_id",nullable=false,length=500)private String publicId;
 @Column(name="original_name",nullable=false,length=255)private String originalName;
 @Column(name="content_type",nullable=false,length=100)private String contentType;
 @Column(name="created_at",nullable=false,updatable=false)private Instant createdAt;
 @PrePersist void create(){createdAt=Instant.now();}
 public Long getId(){return id;}public SupportTicket getTicket(){return ticket;}public void setTicket(SupportTicket v){ticket=v;}public String getSecureUrl(){return secureUrl;}public void setSecureUrl(String v){secureUrl=v;}public String getPublicId(){return publicId;}public void setPublicId(String v){publicId=v;}public String getOriginalName(){return originalName;}public void setOriginalName(String v){originalName=v;}public String getContentType(){return contentType;}public void setContentType(String v){contentType=v;}public Instant getCreatedAt(){return createdAt;}
}
