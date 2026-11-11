package com.iare.club.controller;
import com.iare.club.dto.RegistrationRequest;
import com.iare.club.entity.AppUser;
import com.iare.club.service.UserService;
import com.iare.club.service.WalletService;
import com.iare.club.service.ReferralService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller public class PageController {
    private final UserService service; private final WalletService wallets;
    public PageController(UserService service,WalletService wallets){this.service=service;this.wallets=wallets;}
    @GetMapping("/") String home(){return "index";}
    @GetMapping("/signup") String signup(Model model,Authentication auth,@RequestParam(required=false)String ref){
        if(isAuthenticated(auth))return "redirect:/dashboard";
        if(!model.containsAttribute("registrationRequest")){RegistrationRequest request=new RegistrationRequest();request.setReferralCode(ref);model.addAttribute("registrationRequest",request);} return "signup";
    }
    @PostMapping("/signup") String register(@Valid @ModelAttribute RegistrationRequest registrationRequest,BindingResult binding,RedirectAttributes redirect){
        if(binding.hasErrors())return "signup";
        try{service.register(registrationRequest);}catch(UserService.EmailAlreadyExistsException e){binding.rejectValue("email","duplicate","An account with this email already exists");return "signup";}catch(ReferralService.ReferralException e){binding.rejectValue("referralCode","invalid",e.getMessage());return "signup";}
        redirect.addFlashAttribute("registrationSuccess","Account created successfully. Sign in to continue."); return "redirect:/login";
    }
    @GetMapping("/login") String login(Authentication auth){return isAuthenticated(auth)?"redirect:/dashboard":"login";}
    @GetMapping("/dashboard") String dashboard(Authentication auth,Model model){AppUser user=service.findByEmail(auth.getName());model.addAttribute("user",user);model.addAttribute("wallet",wallets.getOrCreate(user));return "dashboard";}
    private boolean isAuthenticated(Authentication a){return a!=null&&a.isAuthenticated()&&!(a instanceof AnonymousAuthenticationToken);}
}
