package com.iare.club.service;

import com.iare.club.entity.InvestmentProduct;
import com.iare.club.repository.ProductRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    public ProductService(ProductRepository products) { this.products = products; }

    @Cacheable(cacheNames = "products", sync = true)
    @Transactional(readOnly = true)
    public List<InvestmentProduct> findAll() { return List.copyOf(products.findAllByArchivedFalseOrderByCreatedAtDesc()); }

    @Cacheable(cacheNames = "activeProducts", sync = true)
    @Transactional(readOnly = true)
    public List<InvestmentProduct> findActive() { return List.copyOf(products.findByActiveTrueAndArchivedFalseOrderByCreatedAtDesc()); }

    @Transactional(readOnly = true)
    public InvestmentProduct find(long id) { return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id)); }

    @CacheEvict(cacheNames = {"products", "activeProducts"}, allEntries = true)
    @Transactional
    public InvestmentProduct save(InvestmentProduct product) { normalize(product); return products.save(product); }

    @CacheEvict(cacheNames = {"products", "activeProducts"}, allEntries = true)
    @Transactional
    public InvestmentProduct update(long id, InvestmentProduct input) {
        InvestmentProduct product = find(id);
        product.setName(input.getName()); product.setImageUrl(input.getImageUrl());
        product.setOriginalPrice(input.getOriginalPrice()); product.setDiscountPrice(input.getDiscountPrice());
        product.setDailyEarnings(input.getDailyEarnings()); product.setTotalDays(input.getTotalDays());
        product.setActive(input.isActive()); normalize(product); return products.save(product);
    }

    @CacheEvict(cacheNames = {"products", "activeProducts"}, allEntries = true)
    @Transactional
    public void delete(long id) { InvestmentProduct product=find(id); product.setActive(false); product.setArchived(true); products.save(product); }

    @CacheEvict(cacheNames = {"products", "activeProducts"}, allEntries = true)
    @Transactional
    public InvestmentProduct toggleActive(long id) { InvestmentProduct product = find(id); product.setActive(!product.isActive()); return products.save(product); }

    public long count() { return products.count(); }
    public long countActive() { return products.countByActiveTrueAndArchivedFalse(); }
    private void normalize(InvestmentProduct product) { product.setName(product.getName().strip().replaceAll("\\s+", " ")); product.setImageUrl(product.getImageUrl().strip()); }

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(long id) { super("Product " + id + " was not found"); }
    }
}
