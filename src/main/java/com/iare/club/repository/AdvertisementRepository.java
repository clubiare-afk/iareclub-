package com.iare.club.repository;

import com.iare.club.entity.Advertisement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findAllByOrderByUpdatedAtDesc();
    Optional<Advertisement> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
