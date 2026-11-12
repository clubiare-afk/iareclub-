package com.iare.club;

import static org.junit.jupiter.api.Assertions.*;
import com.iare.club.entity.*;import com.iare.club.repository.UserRepository;import com.iare.club.service.SupportTicketService;
import java.util.List;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SupportTicketWorkflowTests {
 @Autowired SupportTicketService support; @Autowired UserRepository users;
 private AppUser user(String email){AppUser user=new AppUser();user.setFullName("Support User");user.setEmail(email);user.setPasswordHash("test-hash");return users.saveAndFlush(user);}
 @Test void completeUserToAdminConversationAndOwnershipWorkflow(){
  AppUser owner=user("support-owner@example.com"),other=user("support-other@example.com");
  SupportTicket ticket=support.create(owner,"Deposit not visible","My verified deposit is not visible in the wallet.","test-request-1",List.of());
  assertEquals(SupportTicket.Status.OPEN,ticket.getStatus());
  assertEquals(1,support.userTickets(owner).size());
  assertEquals(ticket.getId(),support.create(owner,"Ignored duplicate","Duplicate submission","test-request-1",List.of()).getId());
  assertEquals(1,support.userTickets(owner).size());
  assertThrows(SupportTicketService.NotFound.class,()->support.userTicket(other,ticket.getId()));
  support.adminReply(ticket.getId(),"admin@example.com","We are checking the payment reference.");
  SupportTicket afterReply=support.userTicket(owner,ticket.getId());
  assertEquals(SupportTicket.Status.IN_PROGRESS,afterReply.getStatus());
  assertEquals(1,afterReply.getMessages().size());
  support.userReply(owner,ticket.getId(),"Thank you. I can provide more details.");
  support.updateStatus(ticket.getId(),SupportTicket.Status.RESOLVED,"admin@example.com");
  SupportTicket resolved=support.userTicket(owner,ticket.getId());
  assertEquals(2,resolved.getMessages().size());
  assertEquals(SupportTicket.Status.RESOLVED,resolved.getStatus());
  assertEquals(3,resolved.getStatusHistory().size());
  support.updateStatus(ticket.getId(),SupportTicket.Status.CLOSED,"admin@example.com");
  assertThrows(SupportTicketService.InvalidTicket.class,()->support.userReply(owner,ticket.getId(),"New reply"));
 }
}
