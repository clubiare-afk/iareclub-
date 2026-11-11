package com.iare.club.controller;

import com.iare.club.repository.UserRepository;
import com.iare.club.service.AdminWalletService;
import java.math.BigDecimal;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminWalletController {
    private final UserRepository users;

    public AdminWalletController(UserRepository users) { this.users = users; }

    @GetMapping("/admin/wallets")
    String wallets(@RequestParam(defaultValue = "") String q,
                   @RequestParam(defaultValue = "ALL") String balance,
                   @RequestParam(defaultValue = "0") int page, Model model) {
        String filter = switch (balance.toUpperCase()) { case "POSITIVE", "ZERO" -> balance.toUpperCase(); default -> "ALL"; };
        var result = users.searchWalletUsers(q.strip(), filter, PageRequest.of(Math.max(0, page), 20));
        model.addAttribute("page", result); model.addAttribute("q", q); model.addAttribute("balanceFilter", filter);
        return "admin/wallets";
    }
}

@RestController
@RequestMapping("/admin/api/wallets")
class AdminWalletApiController {
    private final AdminWalletService walletService;

    AdminWalletApiController(AdminWalletService walletService) { this.walletService = walletService; }

    @PostMapping("/{userId}/credits")
    ResponseEntity<?> credit(@PathVariable long userId, @RequestBody CreditRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(walletService.credit(userId, request.amount(), request.requestId(), authentication.getName()));
        } catch (AdminWalletService.CreditException exception) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(exception.getMessage()));
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("The credit could not be saved. Refresh the page and try again."));
        }
    }

    record CreditRequest(BigDecimal amount, String requestId) {}
    record ErrorResponse(String message) {}
}
