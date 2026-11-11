package com.iare.club.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scratch_cooldowns")
public class ScratchCooldown {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "next_available_at", nullable = false)
    private Instant nextAvailableAt;

    protected ScratchCooldown() {
    }

    public ScratchCooldown(AppUser user, Instant nextAvailableAt) {
        this.user = user;
        this.nextAvailableAt = nextAvailableAt;
    }

    public void start(Instant nextAvailableAt) {
        this.nextAvailableAt = nextAvailableAt;
    }

    public Instant getNextAvailableAt() {
        return nextAvailableAt;
    }
}
