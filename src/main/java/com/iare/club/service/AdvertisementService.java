package com.iare.club.service;

import com.iare.club.entity.Advertisement;
import com.iare.club.repository.AdvertisementRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvertisementService {
    private final AdvertisementRepository advertisements;

    public AdvertisementService(AdvertisementRepository advertisements) {
        this.advertisements = advertisements;
    }

    @Transactional(readOnly = true)
    public List<Advertisement> findAll() {
        return List.copyOf(advertisements.findAllByOrderByUpdatedAtDesc());
    }

    @Transactional(readOnly = true)
    public Optional<Advertisement> findLatestActive() {
        return advertisements.findFirstByActiveTrueOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Advertisement find(long id) {
        return advertisements.findById(id).orElseThrow(() -> new AdvertisementNotFoundException(id));
    }

    @Transactional
    public Advertisement create(Advertisement advertisement) {
        normalize(advertisement);
        return advertisements.save(advertisement);
    }

    @Transactional
    public Advertisement update(long id, Advertisement input) {
        Advertisement advertisement = find(id);
        advertisement.setImageUrl(input.getImageUrl());
        advertisement.setActive(input.isActive());
        normalize(advertisement);
        return advertisements.save(advertisement);
    }

    @Transactional
    public Advertisement toggle(long id) {
        Advertisement advertisement = find(id);
        advertisement.setActive(!advertisement.isActive());
        return advertisements.save(advertisement);
    }

    @Transactional
    public void delete(long id) {
        if (!advertisements.existsById(id)) throw new AdvertisementNotFoundException(id);
        advertisements.deleteById(id);
    }

    private void normalize(Advertisement advertisement) {
        if (advertisement.getImageUrl() != null) advertisement.setImageUrl(advertisement.getImageUrl().strip());
    }

    public static class AdvertisementNotFoundException extends RuntimeException {
        public AdvertisementNotFoundException(long id) { super("Advertisement " + id + " was not found"); }
    }
}
