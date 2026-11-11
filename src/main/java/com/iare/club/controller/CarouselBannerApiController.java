package com.iare.club.controller;

import com.iare.club.service.CarouselBannerService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carousel-banners")
public class CarouselBannerApiController {
    private final CarouselBannerService banners;

    public CarouselBannerApiController(CarouselBannerService banners) { this.banners = banners; }

    @GetMapping("/active")
    ResponseEntity<List<ActiveBannerResponse>> active() {
        List<ActiveBannerResponse> response = banners.findActive().stream()
                .map(b -> new ActiveBannerResponse(b.getId(), b.getImageUrl(), b.getDisplayOrder(), b.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    public record ActiveBannerResponse(long id, String imageUrl, int displayOrder, Instant updatedAt) {}
}
