package com.iare.club.controller;

import com.iare.club.entity.AppUser;
import com.iare.club.service.InvestmentService;
import com.iare.club.service.UserService;
import com.iare.club.service.WalletService;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investments")
public class InvestmentApiController {
    private final UserService users;
    private final InvestmentService investments;
    private final WalletService wallets;

    public InvestmentApiController(UserService users, InvestmentService investments, WalletService wallets) {
        this.users = users; this.investments = investments; this.wallets = wallets;
    }

    @PostMapping("/{productId}")
    ResponseEntity<?> purchase(@PathVariable long productId, @RequestBody PurchaseRequest request,
                               Authentication authentication) {
        AppUser user = users.findByEmail(authentication.getName());
        try {
            var investment = investments.purchase(user, productId, request.requestReference());
            BigDecimal balance = wallets.getOrCreate(user).getDepositBalance();
            return ResponseEntity.ok(new PurchaseResponse(investment.getProduct().getId(), investment.getId(),
                    balance, "Product purchased successfully and added to My Products."));
        } catch (InvestmentService.DuplicatePurchaseException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
        } catch (InvestmentService.InsufficientDepositException exception) {
            return ResponseEntity.unprocessableEntity().body(new ErrorResponse("Your deposit balance is insufficient for this investment."));
        } catch (InvestmentService.FinanceException exception) {
            return ResponseEntity.unprocessableEntity().body(new ErrorResponse(exception.getMessage()));
        }
    }

    record PurchaseRequest(String requestReference) {}
    record PurchaseResponse(long productId, long investmentId, BigDecimal depositBalance, String message) {}
    record ErrorResponse(String message) {}
}
