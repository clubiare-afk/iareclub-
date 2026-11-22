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
