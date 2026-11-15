package com.iare.club.controller;
import com.iare.club.entity.UpiPaymentRequest;import com.iare.club.service.*;import java.math.BigDecimal;import java.util.Map;import org.springframework.http.*;import org.springframework.security.core.Authentication;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/payments/upi") public class UpiPaymentController{
 private final UpiPaymentService payments;private final UserService users;private final WalletService wallets;
 public UpiPaymentController(UpiPaymentService p,UserService u,WalletService w){payments=p;users=u;wallets=w;}
 @GetMapping("/{reference}")String checkout(@PathVariable String reference,Authentication auth,Model model){var user=users.findByEmail(auth.getName());UpiPaymentRequest p=payments.forUser(reference,user);model.addAttribute("payment",p);model.addAttribute("upiUri",payments.uri(p));model.addAttribute("wallet",wallets.getOrCreate(user));model.addAttribute("autoCreditEnabled",payments.isAuthorizedAutoCredit());return "finance/upi-checkout";}
 @GetMapping(value="/{reference}/qr",produces=MediaType.IMAGE_PNG_VALUE)@ResponseBody byte[] qr(@PathVariable String reference,Authentication auth){return payments.qr(payments.forUser(reference,users.findByEmail(auth.getName())));}
 @GetMapping("/{reference}/status")@ResponseBody Map<String,Object> status(@PathVariable String reference,Authentication auth){UpiPaymentRequest p=payments.forUser(reference,users.findByEmail(auth.getName()));return Map.of("reference",p.getReference(),"status",p.getStatus().name(),"credited",p.getWalletCreditedAt()!=null,"amount",p.getAmount());}
}
