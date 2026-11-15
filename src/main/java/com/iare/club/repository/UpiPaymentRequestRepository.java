package com.iare.club.repository;import com.iare.club.entity.UpiPaymentRequest;import jakarta.persistence.LockModeType;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface UpiPaymentRequestRepository extends JpaRepository<UpiPaymentRequest,Long>{
 Optional<UpiPaymentRequest> findByReference(String reference);List<UpiPaymentRequest> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select p from UpiPaymentRequest p join fetch p.user join fetch p.deposit where p.reference=:reference")Optional<UpiPaymentRequest> findByReferenceForUpdate(@Param("reference")String reference);
 @EntityGraph(attributePaths={"user","deposit"})Page<UpiPaymentRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
 @EntityGraph(attributePaths={"user","deposit"})@Query("select p from UpiPaymentRequest p where (:status is null or p.status=:status) and (:q='' or lower(p.reference) like lower(concat('%',:q,'%')) or lower(p.user.email) like lower(concat('%',:q,'%')) or lower(p.user.fullName) like lower(concat('%',:q,'%')) or lower(coalesce(p.bankReference,'')) like lower(concat('%',:q,'%'))) order by p.createdAt desc")Page<UpiPaymentRequest> search(@Param("status")UpiPaymentRequest.Status status,@Param("q")String q,Pageable pageable);
 long countByStatus(UpiPaymentRequest.Status status);
}
