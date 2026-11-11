package com.iare.club.repository;
import com.iare.club.entity.WalletTransaction;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,Long>{
 List<WalletTransaction> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
 Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId,Pageable pageable);
 @Query("select t from WalletTransaction t where t.user.id=:userId and (:type is null or t.type=:type) and (:status is null or t.status=:status) order by t.createdAt desc")
 Page<WalletTransaction> search(@Param("userId")Long userId,@Param("type")WalletTransaction.Type type,@Param("status")WalletTransaction.Status status,Pageable pageable);
 boolean existsByReference(String reference);
 Optional<WalletTransaction> findByReference(String reference);
}
