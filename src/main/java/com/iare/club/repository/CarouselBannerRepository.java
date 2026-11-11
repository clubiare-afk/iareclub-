package com.iare.club.repository;

import com.iare.club.entity.CarouselBanner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarouselBannerRepository extends JpaRepository<CarouselBanner, Long> {
    List<CarouselBanner> findAllByOrderByDisplayOrderAscIdAsc();
    List<CarouselBanner> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
