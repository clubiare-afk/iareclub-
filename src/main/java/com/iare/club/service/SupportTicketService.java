package com.iare.club.service;

import com.iare.club.entity.*;import com.iare.club.repository.*;
import java.security.SecureRandom;import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;
import org.springframework.data.domain.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.multipart.MultipartFile;

@Service
public class SupportTicketService{
 private final SupportTicketRepository tickets;private final SupportMessageRepository messages;private final SupportStatusHistoryRepository histories;private final CloudinaryStorageService storage;private final SupportTicketPersistenceService persistence;private final SecureRandom random=new SecureRandom();
 public SupportTicketService(SupportTicketRepository t,SupportMessageRepository m,SupportStatusHistoryRepository h,CloudinaryStorageService s,SupportTicketPersistenceService persistence){tickets=t;messages=m;histories=h;storage=s;this.persistence=persistence;}
 public SupportTicket create(AppUser user,String title,String description,String requestKey,List<MultipartFile> files){
  String cleanTitle=required(title,"Complaint title",150),cleanDescription=required(description,"Description",5000);
  String key=required(requestKey,"Submission reference",64);
  var created=persistence.createBase(user,newNumber(),key,cleanTitle,cleanDescription);
  if(!created.created())return created.ticket();
  List<CloudinaryStorageService.Upload> uploads=List.of();
  try{uploads=storage.upload(files,created.ticket().getTicketNumber());return persistence.complete(created.ticket().getId(),uploads,user.getEmail());}
  catch(RuntimeException failure){storage.discard(uploads);try{persistence.discard(created.ticket().getId());}catch(RuntimeException ignored){}throw failure;}
 }
 @Transactional(readOnly=true) public List<SupportTicket> userTickets(AppUser user){return tickets.findByUserIdOrderByUpdatedAtDesc(user.getId());}
 @Transactional(readOnly=true) public SupportTicket userTicket(AppUser user,long id){return hydrate(tickets.findByIdAndUserId(id,user.getId()).orElseThrow(NotFound::new));}
 @Transactional(readOnly=true) public SupportTicket adminTicket(long id){return hydrate(tickets.findById(id).orElseThrow(NotFound::new));}
 @Transactional(readOnly=true) public Page<SupportTicket> adminTickets(SupportTicket.Status status,String q,int page){return tickets.search(status,normalizeQuery(q),PageRequest.of(Math.max(0,page),25));}
 @Transactional public void userReply(AppUser user,long id,String text){SupportTicket ticket=userTicket(user,id);if(ticket.getStatus()==SupportTicket.Status.CLOSED)throw new InvalidTicket("Closed tickets cannot receive new replies.");saveMessage(ticket,SupportMessage.Sender.USER,user.getFullName(),text);}
 @Transactional public void adminReply(long id,String admin,String text){SupportTicket ticket=adminTicket(id);saveMessage(ticket,SupportMessage.Sender.ADMIN,admin,text);if(ticket.getStatus()==SupportTicket.Status.OPEN)changeStatus(ticket,SupportTicket.Status.IN_PROGRESS,admin);}
 @Transactional public void updateStatus(long id,SupportTicket.Status status,String admin){if(status==null)throw new InvalidTicket("Choose a valid ticket status.");changeStatus(adminTicket(id),status,admin);}
 private void saveMessage(SupportTicket ticket,SupportMessage.Sender sender,String name,String text){SupportMessage m=new SupportMessage();m.setTicket(ticket);m.setSender(sender);m.setSenderName(name);m.setMessage(required(text,"Reply",5000));messages.save(m);ticket.getMessages().add(m);ticket.touch();tickets.save(ticket);}
 private void changeStatus(SupportTicket ticket,SupportTicket.Status next,String actor){SupportTicket.Status previous=ticket.getStatus();if(previous==next)return;ticket.setStatus(next);tickets.save(ticket);recordStatus(ticket,previous,next,actor);}
 private void recordStatus(SupportTicket ticket,SupportTicket.Status previous,SupportTicket.Status next,String actor){SupportStatusHistory h=new SupportStatusHistory();h.setTicket(ticket);h.setPreviousStatus(previous);h.setNewStatus(next);h.setChangedBy(actor);histories.save(h);ticket.getStatusHistory().add(h);}
 private String required(String value,String field,int max){String clean=Optional.ofNullable(value).orElse("").strip();if(clean.isBlank())throw new InvalidTicket(field+" is required.");if(clean.length()>max)throw new InvalidTicket(field+" cannot exceed "+max+" characters.");return clean;}
 private String normalizeQuery(String q){return Optional.ofNullable(q).orElse("").strip();}
 private SupportTicket hydrate(SupportTicket ticket){ticket.getMessages().size();ticket.getImages().size();ticket.getStatusHistory().size();ticket.getUser().getEmail();return ticket;}
 private String newNumber(){return "SUP-"+DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now())+"-"+String.format("%06d",random.nextInt(1_000_000));}
 public static class NotFound extends RuntimeException{public NotFound(){super("Support ticket was not found.");}}
 public static class InvalidTicket extends RuntimeException{public InvalidTicket(String message){super(message);}}
}
