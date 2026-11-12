package com.iare.club.controller;
import com.iare.club.entity.AppUser;import com.iare.club.service.*;import java.util.*;import org.springframework.security.core.Authentication;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/support")
public class SupportController{
 private final SupportTicketService support;private final UserService users;
 public SupportController(SupportTicketService support,UserService users){this.support=support;this.users=users;}
 private AppUser user(Authentication auth){return users.findByEmail(auth.getName());}
 @GetMapping String list(Authentication auth,Model model){AppUser user=user(auth);model.addAttribute("user",user);model.addAttribute("tickets",support.userTickets(user));return "support/index";}
 @GetMapping("/new")String form(Authentication auth,Model model){model.addAttribute("user",user(auth));if(!model.containsAttribute("requestKey"))model.addAttribute("requestKey",UUID.randomUUID().toString());return "support/new";}
 @PostMapping String create(Authentication auth,@RequestParam String title,@RequestParam String description,@RequestParam String requestKey,@RequestParam(name="images",required=false)List<MultipartFile> images,RedirectAttributes flash){
  try{var ticket=support.create(user(auth),title,description,requestKey,images);flash.addFlashAttribute("success","Ticket "+ticket.getTicketNumber()+" was created successfully.");return "redirect:/support/"+ticket.getId();}
  catch(RuntimeException e){flash.addFlashAttribute("error",e.getMessage());flash.addFlashAttribute("title",title);flash.addFlashAttribute("description",description);flash.addFlashAttribute("requestKey",requestKey);return "redirect:/support/new";}
 }
 @GetMapping("/{id}")String detail(Authentication auth,@PathVariable long id,Model model){model.addAttribute("user",user(auth));model.addAttribute("ticket",support.userTicket(user(auth),id));return "support/detail";}
 @PostMapping("/{id}/replies")String reply(Authentication auth,@PathVariable long id,@RequestParam String message,RedirectAttributes flash){try{support.userReply(user(auth),id,message);flash.addFlashAttribute("success","Your reply was added.");}catch(RuntimeException e){flash.addFlashAttribute("error",e.getMessage());}return "redirect:/support/"+id;}
}
