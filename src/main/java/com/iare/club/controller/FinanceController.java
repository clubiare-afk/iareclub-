
package com.iare.club.controller;

import com.iare.club.dto.*;
import com.iare.club.entity.*;
import com.iare.club.service.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class FinanceController {

    private final UserService users;
    private final WalletService wallets;
    private final DepositService deposits;
    private final InvestmentService investments;
    private final WithdrawalService withdrawals;
    private final ProductService products;
    private final ZapUpiService zapUpi;
    private final PaymentOriginService paymentOrigins;

    public FinanceController(
            UserService u,
            WalletService w,
            DepositService d,
            InvestmentService i,
            WithdrawalService x,
            ProductService products,
            ZapUpiService zapUpi,
            PaymentOriginService paymentOrigins) {

        users = u;
        wallets = w;
        deposits = d;
        investments = i;
        withdrawals = x;
        this.products = products;
        this.zapUpi = zapUpi;
        this.paymentOrigins = paymentOrigins;
    }

    private AppUser user(Authentication auth) {
        return users.findByEmail(auth.getName());
    }

    @GetMapping("/wallet")
    String wallet(Authentication auth, Model model) {
        AppUser u = user(auth);

        model.addAttribute("user", u);
        model.addAttribute("wallet", wallets.getOrCreate(u));
        model.addAttribute("transactions", wallets.history(u));

        return "finance/wallet";
    }

    @GetMapping("/deposit")
    String deposit(
            Authentication auth,
            @RequestParam(required = false) String payment,
            @RequestParam(name = "order_id", required = false) String orderId,
            Model model) {

        AppUser u = user(auth);

        if (!model.containsAttribute("depositRequest")) {
            model.addAttribute("depositRequest", new DepositRequest());
        }

        if ("success".equals(payment) && orderId != null) {
            deposits.findSuccessfulForUser(orderId, u).ifPresent(value -> {
                model.addAttribute("successPayment", value);
                model.addAttribute(
                    "success",
                    "Payment Successful! ₹" +
                    value.getAmount().setScale(2) +
                    " has been added to your wallet."
                );
            });
        } else if ("pending".equals(payment)
                && orderId != null
                && !orderId.isBlank()) {

            model.addAttribute(
                "success",
                "Payment is pending authenticated verification. Your wallet has not been credited yet."
            );
            model.addAttribute("pendingTransaction", orderId);

        } else if ("failed".equals(payment)) {
            model.addAttribute(
                "error",
                "Payment was not completed. Your wallet was not charged."
            );
        }

        model.addAttribute("wallet", wallets.getOrCreate(u));
        model.addAttribute("deposits", deposits.history(u));
        model.addAttribute("paymentEnabled", zapUpi.isEnabled());

        return "finance/deposit";
    }

    @PostMapping("/deposit")
    String createDeposit(
            Authentication auth,
            @Valid @ModelAttribute DepositRequest depositRequest,
            BindingResult result,
            Model model,
            RedirectAttributes flash,
            HttpServletRequest request) {

        if (result.hasErrors()) {
            return deposit(auth, null, null, model);
        }

        try {
            ZapUpiService.Checkout checkout = zapUpi.initiate(
                user(auth),
                depositRequest.getAmount(),
                paymentOrigins.trustedOrigin(request)
            );

            return "redirect:" + checkout.paymentUrl();

        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/deposit";
        }
    }

    @GetMapping("/investments")
    String investments(Authentication auth, Model model) {
        AppUser u = user(auth);

        model.addAttribute("wallet", wallets.getOrCreate(u));
        model.addAttribute("investments", investments.findFor(u));
        model.addAttribute("products", products.findActive());
        model.addAttribute(
            "requestReference",
            investments.newRequestReference()
        );
        model.addAttribute("now", investments.now());

        return "finance/investments";
    }

    // ================================================
    // UPDATED: PRODUCTS SORTED BY DISCOUNT PRICE ASC
    // Lowest discount price first, highest price last
    // ================================================

    @GetMapping("/products")
    String products(Authentication auth, Model model) {

        AppUser u = user(auth);

        model.addAttribute("wallet", wallets.getOrCreate(u));

        // Fetch active products added by admin
        List<Product> sortedProducts =
            new ArrayList<>(products.findActive());

        // Sort by discount price in ascending order
        sortedProducts.sort(
            Comparator.comparing(Product::getDiscountPrice)
        );

        // Send sorted products to Thymeleaf
        model.addAttribute("products", sortedProducts);

        // Preserve purchased product status
        model.addAttribute(
            "purchasedProductIds",
            investments.purchasedProductIds(u)
        );

        // Preserve purchase request reference
        model.addAttribute(
            "requestReference",
            investments.newRequestReference()
        );

        return "finance/products";
    }

    @GetMapping("/my-products")
    String myProducts(Authentication auth, Model model) {
        AppUser u = user(auth);

        model.addAttribute("wallet", wallets.getOrCreate(u));
        model.addAttribute("investments", investments.findFor(u));
        model.addAttribute("now", investments.now());

        return "finance/my-products";
    }

    private String investmentTarget(String returnTo) {
        return switch (returnTo == null ? "" : returnTo) {
            case "dashboard" -> "/dashboard";
            case "products" -> "/products";
            case "my-products" -> "/my-products";
            default -> "/investments";
        };
    }

    @PostMapping("/investments/{productId}")
    String purchase(
            Authentication auth,
            @PathVariable long productId,
            @RequestParam String requestReference,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes flash,
            HttpServletRequest request) {

        String target = investmentTarget(returnTo);

        try {
            ZapUpiService.Checkout checkout =
                zapUpi.initiateProduct(
                    user(auth),
                    productId,
                    requestReference,
                    paymentOrigins.trustedOrigin(request)
                );

            return "redirect:" + checkout.paymentUrl();

        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:" + target;
        }
    }

    @PostMapping("/investments/{id}/collect")
    String collect(
            Authentication auth,
            @PathVariable long id,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes flash) {

        String target = investmentTarget(returnTo);

        try {
            investments.collectReward(user(auth), id);

            flash.addFlashAttribute(
                "success",
                "Reward collected into your available withdrawal balance."
            );

        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:" + target;
    }

    @GetMapping("/withdrawal")
    String withdrawal(Authentication auth, Model model) {
        AppUser u = user(auth);

        if (!model.containsAttribute("bankRequest")) {
            model.addAttribute("bankRequest", new BankAccountRequest());
        }

        if (!model.containsAttribute("withdrawalRequest")) {
            model.addAttribute(
                "withdrawalRequest",
                new WithdrawalRequestDto()
            );
        }

        model.addAttribute("wallet", wallets.getOrCreate(u));
        model.addAttribute(
            "bank",
            withdrawals.bankFor(u).orElse(null)
        );
        model.addAttribute("withdrawals", withdrawals.history(u));
        model.addAttribute("minimum", withdrawals.getMinimum());
        model.addAttribute(
            "nextWithdrawalAt",
            withdrawals.nextWithdrawalAt(u).orElse(null)
        );
        model.addAttribute("now", withdrawals.now());

        return "finance/withdrawal";
    }

    @PostMapping("/withdrawal/bank")
    String saveBank(
            Authentication auth,
            @Valid @ModelAttribute("bankRequest") BankAccountRequest dto,
            BindingResult result,
            Model model,
            RedirectAttributes flash) {

        if (result.hasErrors()) {
            return withdrawal(auth, model);
        }

        try {
            withdrawals.saveBank(user(auth), dto);
            flash.addFlashAttribute(
                "success",
                "Bank account saved securely."
            );

        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/withdrawal";
    }

    @PostMapping("/withdrawal")
    String requestWithdrawal(
            Authentication auth,
            @Valid @ModelAttribute("withdrawalRequest") WithdrawalRequestDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes flash) {

        if (result.hasErrors()) {
            return withdrawal(auth, model);
        }

        try {
            withdrawals.request(user(auth), dto.getAmount());

            flash.addFlashAttribute(
                "success",
                "Withdrawal request submitted and the amount has been reserved."
            );

        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/withdrawal";
    }

    @GetMapping("/activity")
    String activity(
            Authentication auth,
            @RequestParam(required = false) WalletTransaction.Type type,
            @RequestParam(required = false) WalletTransaction.Status status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        AppUser u = user(auth);

        var result = wallets.activity(u, type, status, page);

        model.addAttribute("transactions", result.getContent());
        model.addAttribute("page", result);
        model.addAttribute("types", WalletTransaction.Type.values());
        model.addAttribute("statuses", WalletTransaction.Status.values());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);

        return "finance/activity";
    }
}
