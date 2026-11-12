package com.iare.club.repository;
import com.iare.club.entity.InvestmentProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductRepository extends JpaRepository<InvestmentProduct,Long>{
    List<InvestmentProduct> findAllByOrderByCreatedAtDesc();
    List<InvestmentProduct> findAllByArchivedFalseOrderByCreatedAtDesc();
    List<InvestmentProduct> findByActiveTrueAndArchivedFalseOrderByCreatedAtDesc();
    long countByActiveTrueAndArchivedFalse();
}
