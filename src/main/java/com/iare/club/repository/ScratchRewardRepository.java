package com.iare.club.repository;
import com.iare.club.entity.ScratchReward;import java.math.BigDecimal;import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface ScratchRewardRepository extends JpaRepository<ScratchReward,Long>{
 List<ScratchReward> findTop100ByUserIdOrderByScratchedAtDesc(Long userId);
 @Query("select coalesce(sum(s.amount),0) from ScratchReward s where s.user.id=:userId") BigDecimal totalForUser(@Param("userId")Long userId);
}
