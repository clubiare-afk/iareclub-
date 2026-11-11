package com.iare.club.repository;
import com.iare.club.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface BankAccountRepository extends JpaRepository<BankAccount,Long>{Optional<BankAccount> findByUserId(Long userId);}
