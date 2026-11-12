package com.iare.club.repository;
import com.iare.club.entity.SupportTicket;import java.util.*;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,Long>{
 List<SupportTicket> findByUserIdOrderByUpdatedAtDesc(Long userId);
 Optional<SupportTicket> findByIdAndUserId(Long id,Long userId);
 Optional<SupportTicket> findByUserIdAndRequestKey(Long userId,String requestKey);
 @EntityGraph(attributePaths={"user"})
 @Query("select t from SupportTicket t where (:status is null or t.status=:status) and (:q='' or lower(t.user.fullName) like lower(concat('%',:q,'%')) or lower(t.user.email) like lower(concat('%',:q,'%')) or lower(t.ticketNumber) like lower(concat('%',:q,'%')) or lower(t.title) like lower(concat('%',:q,'%'))) order by t.updatedAt desc")
 Page<SupportTicket> search(@Param("status")SupportTicket.Status status,@Param("q")String q,Pageable pageable);
}
