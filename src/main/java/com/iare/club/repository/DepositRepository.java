package com.iare.club.repository;
import com.iare.club.entity.Deposit;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface DepositRepository extends JpaRepository<Deposit,Long>{
 List<Deposit> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
 Optional<Deposit> findByPaymentReference(String reference);
 @EntityGraph(attributePaths="user") @Query("select d from Deposit d where d.id=:id") Optional<Deposit> findDetailById(@Param("id")Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from Deposit d where d.paymentReference=:reference") Optional<Deposit> findByPaymentReferenceForUpdate(@Param("reference")String reference);
 @Query("select d.paymentReference from Deposit d where d.status=:status and d.updatedAt<:before order by d.updatedAt") List<String> findPendingReferences(@Param("status")Deposit.Status status,@Param("before")java.time.Instant before,Pageable pageable);
 Page<Deposit> findAllByOrderByCreatedAtDesc(Pageable pageable);
 Page<Deposit> findByStatusOrderByCreatedAtDesc(Deposit.Status status,Pageable pageable);
 @EntityGraph(attributePaths="user") @Query("select d from Deposit d where (:status is null or d.status=:status) and (:q='' or lower(d.user.fullName) like lower(concat('%',:q,'%')) or lower(d.user.email) like lower(concat('%',:q,'%')) or lower(d.paymentReference) like lower(concat('%',:q,'%')) or lower(coalesce(d.payuPaymentId,'')) like lower(concat('%',:q,'%')) or lower(coalesce(d.bankReference,'')) like lower(concat('%',:q,'%'))) order by d.createdAt desc")
 Page<Deposit> search(@Param("status")Deposit.Status status,@Param("q")String q,Pageable pageable);
 long countByStatus(Deposit.Status status);
}
