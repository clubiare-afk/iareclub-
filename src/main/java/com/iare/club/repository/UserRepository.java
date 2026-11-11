package com.iare.club.repository;
import com.iare.club.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByReferralCode(String referralCode);
    Optional<AppUser> findByReferralCode(String referralCode);
    List<AppUser> findAllByOrderByCreatedAtDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where u.id=:id") Optional<AppUser> findByIdForUpdate(@Param("id") Long id);
    @Query(value="""
        SELECT u.id AS userId, u.full_name AS fullName, u.email AS email,
               COALESCE(w.deposit_balance,0) AS depositBalance,
               COALESCE(w.reward_balance,0) AS rewardBalance,
               COALESCE(w.deposit_balance,0)+COALESCE(w.reward_balance,0) AS currentBalance,
               COALESCE((SELECT SUM(d.amount) FROM deposits d WHERE d.user_id=u.id AND d.status='SUCCESSFUL'),0) AS totalDeposits,
               COALESCE((SELECT SUM(x.amount) FROM withdrawal_requests x WHERE x.user_id=u.id AND x.status='SUCCESSFUL'),0) AS totalWithdrawals
          FROM app_users u LEFT JOIN wallets w ON w.user_id=u.id
         WHERE (:q='' OR LOWER(u.full_name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) OR CAST(u.id AS VARCHAR) LIKE CONCAT('%',:q,'%'))
           AND (:balanceFilter='ALL' OR (:balanceFilter='POSITIVE' AND (COALESCE(w.deposit_balance,0)+COALESCE(w.reward_balance,0))>0) OR (:balanceFilter='ZERO' AND (COALESCE(w.deposit_balance,0)+COALESCE(w.reward_balance,0))=0))
         ORDER BY u.created_at DESC
        """,countQuery="""
        SELECT COUNT(*) FROM app_users u LEFT JOIN wallets w ON w.user_id=u.id
         WHERE (:q='' OR LOWER(u.full_name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) OR CAST(u.id AS VARCHAR) LIKE CONCAT('%',:q,'%'))
           AND (:balanceFilter='ALL' OR (:balanceFilter='POSITIVE' AND (COALESCE(w.deposit_balance,0)+COALESCE(w.reward_balance,0))>0) OR (:balanceFilter='ZERO' AND (COALESCE(w.deposit_balance,0)+COALESCE(w.reward_balance,0))=0))
        """,nativeQuery=true)
    Page<AdminWalletUserView> searchWalletUsers(@Param("q") String q, @Param("balanceFilter") String balanceFilter, Pageable pageable);
}
