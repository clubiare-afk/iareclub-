package com.iare.club.controller;

import com.iare.club.entity.AppUser;
import com.iare.club.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReferralController {
    private final UserService users; private final ReferralService referrals;
    public ReferralController(UserService users,ReferralService referrals){this.users=users;this.referrals=referrals;}
    @GetMapping("/refer-and-earn") String dashboard(Authentication auth,Model model){
        AppUser user=users.findByEmail(auth.getName());referrals.ensureCode(user);
        model.addAttribute("user",user);model.addAttribute("referral",referrals.dashboard(user));model.addAttribute("bonus",referrals.getBonus());return "finance/referrals";
    }
    @GetMapping("/admin/referrals") String admin(@RequestParam(defaultValue="0")int page,Model model){
        model.addAttribute("dashboard",referrals.adminDashboard(page));return "admin/referrals";
    }
    @PostMapping("/admin/referrals/{id}/reverse") String reverse(@PathVariable long id,@RequestParam String reason,RedirectAttributes flash){
        try{referrals.reverse(id,reason);flash.addFlashAttribute("success","Referral bonus reversed and wallet balance updated.");}
        catch(RuntimeException e){flash.addFlashAttribute("error",e.getMessage());}
        return "redirect:/admin/referrals";
    }
}
