package com.iare.club.service;

import com.iare.club.entity.*;
import com.iare.club.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScratchService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final Duration COOLDOWN = Duration.ofHours(24);
    private final ScratchRewardRepository scratches;
    private final ScratchCooldownRepository cooldowns;
    private final UserRepository users;
    private final WalletService walletService;
    private final WalletRepository wallets;
    private final WalletTransactionRepository transactions;
    private final ScratchRewardGenerator generator;
    private final Clock clock;

    public ScratchService(ScratchRewardRepository s, ScratchCooldownRepository cooldowns, UserRepository u,
            WalletService ws, WalletRepository w, WalletTransactionRepository t, ScratchRewardGenerator g, Clock c) {
        scratches = s;
        this.cooldowns = cooldowns;
        users = u;
        walletService = ws;
        wallets = w;
        transactions = t;
        generator = g;
        clock = c;
    }

    @Transactional
    public ScratchReward scratch(AppUser account) {
        AppUser user = users.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new ScratchException("User not found"));
        Instant now = clock.instant();
        Availability availability = availability(user.getId(), now);
        if (!availability.eligible())
            throw new ScratchCooldownException(availability.nextAvailableAt());
        LocalDate day = now.atZone(ZONE).toLocalDate();
        Wallet wallet = walletService.lock(user);
        BigDecimal amount = generator.generate();
        String reference = "SCR-" + user.getId() + "-" + UUID.randomUUID();
        ScratchReward result = scratches.save(new ScratchReward(user, day, amount, reference, now));
        wallet.creditReward(amount);
        wallets.save(wallet);
        transactions.save(
                new WalletTransaction(user, WalletTransaction.Type.SCRATCH_REWARD, WalletTransaction.Status.SUCCESSFUL,
                        amount, wallet.getRewardBalance(), reference, "Scratch & Earn reward", now));
        ScratchCooldown cooldown = cooldowns.findByUserId(user.getId())
                .orElseGet(() -> new ScratchCooldown(user, now.plus(COOLDOWN)));
        cooldown.start(now.plus(COOLDOWN));
        cooldowns.save(cooldown);
        return result;
    }

    @Transactional(readOnly = true)
    public View view(AppUser user) {
        Instant now = clock.instant();
        Availability availability = availability(user.getId(), now);
        return new View(availability.eligible(), availability.nextAvailableAt(), now,
                scratches.findTop100ByUserIdOrderByScratchedAtDesc(user.getId()),
                scratches.totalForUser(user.getId()));
    }

    @Transactional(readOnly = true)
    public Availability availability(AppUser user) {
        return availability(user.getId(), clock.instant());
    }

    private Availability availability(Long userId, Instant now) {
        Optional<ScratchCooldown> cooldown = cooldowns.findByUserId(userId);
        if (cooldown.isEmpty())
            return new Availability(true, null, now);
        Instant next = cooldown.get().getNextAvailableAt();
        return new Availability(!now.isBefore(next), next, now);
    }

    public record View(boolean eligible, Instant nextAvailableAt, Instant serverNow, List<ScratchReward> history,
            BigDecimal totalEarned) {
    }

    public record Availability(boolean eligible, Instant nextAvailableAt, Instant serverNow) {
    }

    public static class ScratchException extends RuntimeException {
        public ScratchException(String m) {
            super(m);
        }
    }

    public static class ScratchCooldownException extends ScratchException {
        private final Instant nextAvailableAt;

        public ScratchCooldownException(Instant nextAvailableAt) {
            super("Your next scratch is available after the 24-hour cooldown");
            this.nextAvailableAt = nextAvailableAt;
        }

        public Instant getNextAvailableAt() {
            return nextAvailableAt;
        }
    }
}
