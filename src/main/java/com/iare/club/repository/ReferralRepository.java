package com.iare.club.repository;

import com.iare.club.entity.Referral;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReferralRepository extends JpaRepository<Referral,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Referral r join fetch r.referrer where r.referredUser.id=:userId")
    Optional<Referral> findByReferredUserIdForUpdate(@Param("userId")Long userId);
    Optional<Referral> findByReferredUserId(Long userId);
    @EntityGraph(attributePaths={"referredUser","qualifyingInvestment"}) List<Referral> findTop100ByReferrerIdOrderByCreatedAtDesc(Long referrerId);
    long countByReferrerId(Long referrerId);
    long countByReferrerIdAndStatus(Long referrerId,Referral.Status status);
    @Query("select coalesce(sum(r.bonusAmount),0) from Referral r where r.referrer.id=:id and r.status='SUCCESSFUL'") BigDecimal totalSuccessfulFor(@Param("id")Long id);
    @EntityGraph(attributePaths={"referrer","referredUser","qualifyingInvestment"}) Page<Referral> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(Referral.Status status);
    @Query("select coalesce(sum(r.bonusAmount),0) from Referral r where r.status='SUCCESSFUL'") BigDecimal totalExpense();
}
