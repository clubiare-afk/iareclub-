package com.iare.club.service;

import com.iare.club.entity.CarouselBanner;
import com.iare.club.repository.CarouselBannerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarouselBannerService {
    private final CarouselBannerRepository repository;

    public CarouselBannerService(CarouselBannerRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<CarouselBanner> findAll() { return List.copyOf(repository.findAllByOrderByDisplayOrderAscIdAsc()); }

    @Transactional(readOnly = true)
    public List<CarouselBanner> findActive() { return List.copyOf(repository.findByActiveTrueOrderByDisplayOrderAscIdAsc()); }

    @Transactional(readOnly = true)
    public CarouselBanner find(long id) {
        return repository.findById(id).orElseThrow(() -> new CarouselBannerNotFoundException(id));
    }

    @Transactional
    public CarouselBanner create(CarouselBanner banner) {
        normalize(banner);
        banner.setDisplayOrder(repository.findAllByOrderByDisplayOrderAscIdAsc().size());
        return repository.save(banner);
    }

    @Transactional
    public CarouselBanner update(long id, CarouselBanner input) {
        CarouselBanner banner = find(id);
        banner.setImageUrl(input.getImageUrl());
        banner.setActive(input.isActive());
        normalize(banner);
        return repository.save(banner);
    }

    @Transactional
    public CarouselBanner toggle(long id) {
        CarouselBanner banner = find(id);
        banner.setActive(!banner.isActive());
        return repository.save(banner);
    }

    @Transactional
    public void move(long id, String direction) {
        List<CarouselBanner> banners = repository.findAllByOrderByDisplayOrderAscIdAsc();
        int index = -1;
        for (int i = 0; i < banners.size(); i++) if (banners.get(i).getId() == id) { index = i; break; }
        if (index < 0) throw new CarouselBannerNotFoundException(id);
        int target = "up".equalsIgnoreCase(direction) ? index - 1 : "down".equalsIgnoreCase(direction) ? index + 1 : index;
        if (target < 0 || target >= banners.size() || target == index) return;
        CarouselBanner current = banners.get(index);
        CarouselBanner other = banners.get(target);
        int currentOrder = current.getDisplayOrder();
        current.setDisplayOrder(other.getDisplayOrder());
        other.setDisplayOrder(currentOrder);
        repository.saveAll(List.of(current, other));
    }

    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) throw new CarouselBannerNotFoundException(id);
        repository.deleteById(id);
        List<CarouselBanner> remaining = repository.findAllByOrderByDisplayOrderAscIdAsc();
        for (int i = 0; i < remaining.size(); i++) remaining.get(i).setDisplayOrder(i);
        repository.saveAll(remaining);
    }

    private void normalize(CarouselBanner banner) {
        if (banner.getImageUrl() != null) banner.setImageUrl(banner.getImageUrl().strip());
    }

    public static class CarouselBannerNotFoundException extends RuntimeException {
        public CarouselBannerNotFoundException(long id) { super("Carousel banner " + id + " was not found"); }
    }
}
