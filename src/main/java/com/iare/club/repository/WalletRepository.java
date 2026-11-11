package com.iare.club.repository;
import com.iare.club.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface WalletRepository extends JpaRepository<Wallet,Long>{
 Optional<Wallet> findByUserId(Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Wallet w where w.user.id=:userId") Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
