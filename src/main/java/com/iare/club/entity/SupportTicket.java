package com.iare.club.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Formula;

@Entity
@Table(name="support_tickets", indexes={
        @Index(name="idx_support_ticket_user_created",columnList="user_id,created_at"),
        @Index(name="idx_support_ticket_status_updated",columnList="status,updated_at")
}, uniqueConstraints=@UniqueConstraint(name="uk_support_ticket_number",columnNames="ticket_number"))
public class SupportTicket {
    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="ticket_number",nullable=false,length=32) private String ticketNumber;
    @Column(name="request_key",length=64) private String requestKey;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private AppUser user;
    @Column(nullable=false,length=150) private String title;
    @Column(nullable=false,columnDefinition="text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.OPEN;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Formula("(select count(si.id) from support_images si where si.ticket_id = id)") private long imageCount;
    @OneToMany(mappedBy="ticket",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("createdAt ASC") private List<SupportMessage> messages=new ArrayList<>();
    @OneToMany(mappedBy="ticket",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("createdAt ASC") private List<SupportImage> images=new ArrayList<>();
    @OneToMany(mappedBy="ticket",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("createdAt ASC") private List<SupportStatusHistory> statusHistory=new ArrayList<>();
    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;}
    @PreUpdate void update(){updatedAt=Instant.now();}
    public void touch(){updatedAt=Instant.now();}
    public Long getId(){return id;} public String getTicketNumber(){return ticketNumber;} public void setTicketNumber(String v){ticketNumber=v;}
    public String getRequestKey(){return requestKey;} public void setRequestKey(String v){requestKey=v;}
    public AppUser getUser(){return user;} public void setUser(AppUser v){user=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;} public Status getStatus(){return status;} public void setStatus(Status v){status=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public List<SupportMessage> getMessages(){return messages;}
    public List<SupportImage> getImages(){return images;} public List<SupportStatusHistory> getStatusHistory(){return statusHistory;}
    public long getImageCount(){return imageCount;}
}
