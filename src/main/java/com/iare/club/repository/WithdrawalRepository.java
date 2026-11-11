package com.iare.club.repository;
import com.iare.club.entity.WithdrawalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface WithdrawalRepository extends JpaRepository<WithdrawalRequest,Long>{
 List<WithdrawalRequest> findTop50ByUserIdOrderByRequestedAtDesc(Long userId);
 Optional<WithdrawalRequest> findFirstByUserIdOrderByRequestedAtDesc(Long userId);
 @EntityGraph(attributePaths={"user","bankAccount"}) @Query("select w from WithdrawalRequest w where w.id=:id") Optional<WithdrawalRequest> findDetailById(@Param("id")Long id);
 Page<WithdrawalRequest> findAllByOrderByRequestedAtDesc(Pageable pageable);
 Page<WithdrawalRequest> findByStatusOrderByRequestedAtDesc(WithdrawalRequest.Status status,Pageable pageable);
 @EntityGraph(attributePaths={"user","bankAccount"}) @Query("select w from WithdrawalRequest w where (:status is null or w.status=:status) and (:q='' or lower(w.user.fullName) like lower(concat('%',:q,'%')) or lower(w.user.email) like lower(concat('%',:q,'%')) or lower(w.reference) like lower(concat('%',:q,'%'))) order by w.requestedAt desc")
 Page<WithdrawalRequest> search(@Param("status")WithdrawalRequest.Status status,@Param("q")String q,Pageable pageable);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths={"user","bankAccount"}) @Query("select w from WithdrawalRequest w where w.id=:id") Optional<WithdrawalRequest> findByIdForUpdate(@Param("id")Long id);
 long countByStatus(WithdrawalRequest.Status status);
}
