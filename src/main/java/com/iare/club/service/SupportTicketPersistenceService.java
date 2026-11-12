package com.iare.club.service;
import com.iare.club.entity.*;import com.iare.club.repository.*;import java.util.*;import org.springframework.dao.DataIntegrityViolationException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service
public class SupportTicketPersistenceService{
 private final SupportTicketRepository tickets;private final SupportImageRepository images;private final SupportStatusHistoryRepository histories;
 public SupportTicketPersistenceService(SupportTicketRepository tickets,SupportImageRepository images,SupportStatusHistoryRepository histories){this.tickets=tickets;this.images=images;this.histories=histories;}
 @Transactional public Created createBase(AppUser user,String number,String requestKey,String title,String description){
  Optional<SupportTicket> existing=tickets.findByUserIdAndRequestKey(user.getId(),requestKey);if(existing.isPresent())return new Created(existing.get(),false);
  SupportTicket ticket=new SupportTicket();ticket.setTicketNumber(number);ticket.setRequestKey(requestKey);ticket.setUser(user);ticket.setTitle(title);ticket.setDescription(description);
  try{return new Created(tickets.saveAndFlush(ticket),true);}catch(DataIntegrityViolationException duplicate){return new Created(tickets.findByUserIdAndRequestKey(user.getId(),requestKey).orElseThrow(()->duplicate),false);}
 }
 @Transactional public SupportTicket complete(long ticketId,List<CloudinaryStorageService.Upload> uploads,String actor){
  SupportTicket ticket=tickets.findById(ticketId).orElseThrow(SupportTicketService.NotFound::new);
  for(var upload:uploads){SupportImage image=new SupportImage();image.setTicket(ticket);image.setSecureUrl(upload.secureUrl());image.setPublicId(upload.publicId());image.setOriginalName(upload.originalName());image.setContentType(upload.contentType());images.save(image);ticket.getImages().add(image);}
  SupportStatusHistory history=new SupportStatusHistory();history.setTicket(ticket);history.setPreviousStatus(null);history.setNewStatus(SupportTicket.Status.OPEN);history.setChangedBy(actor);histories.save(history);ticket.getStatusHistory().add(history);
  return ticket;
 }
 @Transactional public void discard(long ticketId){tickets.deleteById(ticketId);}
 public record Created(SupportTicket ticket,boolean created){}
}
