package com.iare.club.controller;

import com.iare.club.entity.Advertisement;
import com.iare.club.service.AdvertisementService;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advertisements")
public class AdvertisementApiController {
    private final AdvertisementService advertisements;

    public AdvertisementApiController(AdvertisementService advertisements) {
        this.advertisements = advertisements;
    }

    @GetMapping("/active")
    ResponseEntity<ActiveAdvertisementResponse> active() {
        return advertisements.findLatestActive()
                .map(this::response)
                .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build());
    }

    private ResponseEntity<ActiveAdvertisementResponse> response(Advertisement advertisement) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new ActiveAdvertisementResponse(
                        advertisement.getId(), advertisement.getImageUrl(), advertisement.getUpdatedAt()));
    }

    public record ActiveAdvertisementResponse(long id, String imageUrl, Instant updatedAt) {}
}
