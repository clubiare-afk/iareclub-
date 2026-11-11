package com.iare.club.repository;

import com.iare.club.entity.ScratchCooldown;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScratchCooldownRepository extends JpaRepository<ScratchCooldown, Long> {
    Optional<ScratchCooldown> findByUserId(Long userId);
}
