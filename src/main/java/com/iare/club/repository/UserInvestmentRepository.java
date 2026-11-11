package com.iare.club.repository;
import com.iare.club.entity.UserInvestment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface UserInvestmentRepository extends JpaRepository<UserInvestment,Long>{
 List<UserInvestment> findTop100ByUserIdOrderByPurchasedAtDesc(Long userId);
 Optional<UserInvestment> findByRequestReference(String reference);
 boolean existsByUserIdAndProductId(Long userId,Long productId);
 @Query("select i.product.id from UserInvestment i where i.user.id=:userId") Set<Long> findProductIdsByUserId(@Param("userId")Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from UserInvestment i where i.id=:id") Optional<UserInvestment> findByIdForUpdate(@Param("id")Long id);
}
