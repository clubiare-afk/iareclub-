package com.iare.club.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "investment_products")
public class InvestmentProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Product name is required") @Size(max = 120)
    @Column(nullable = false, length = 120) private String name;
    @NotBlank(message = "Image URL is required") @Size(max = 1000)
    @Pattern(regexp = "^https?://.+", message = "Enter a valid HTTP or HTTPS image URL")
    @Column(name = "image_url", nullable = false, length = 1000) private String imageUrl;
    @NotNull @DecimalMin(value = "0.01", message = "Original price must be positive") @Digits(integer = 12, fraction = 2)
    @Column(name = "original_price", nullable = false, precision = 14, scale = 2) private BigDecimal originalPrice;
    @NotNull @DecimalMin(value = "0.01", message = "Discount price must be positive") @Digits(integer = 12, fraction = 2)
    @Column(name = "discount_price", nullable = false, precision = 14, scale = 2) private BigDecimal discountPrice;
    @NotNull @DecimalMin(value = "0.01", message = "Daily earnings must be positive") @Digits(integer = 12, fraction = 2)
    @Column(name = "daily_earnings", nullable = false, precision = 14, scale = 2) private BigDecimal dailyEarnings;
    @NotNull @Min(value = 1, message = "Total days must be at least 1") @Max(value = 3650, message = "Total days cannot exceed 3650")
    @Column(name = "total_days", nullable = false) private Integer totalDays;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private boolean archived = false;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;}
    @PreUpdate void update(){updatedAt=Instant.now();}
    @AssertTrue(message = "Discount price cannot exceed original price")
    public boolean isDiscountValid(){return originalPrice==null||discountPrice==null||discountPrice.compareTo(originalPrice)<=0;}
    @Transient public BigDecimal getTotalReturns(){return dailyEarnings==null||totalDays==null?BigDecimal.ZERO:dailyEarnings.multiply(BigDecimal.valueOf(totalDays));}
    @Transient public BigDecimal getNetProfit(){return discountPrice==null?BigDecimal.ZERO:getTotalReturns().subtract(discountPrice);}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String imageUrl){this.imageUrl=imageUrl;}
    public BigDecimal getOriginalPrice(){return originalPrice;} public void setOriginalPrice(BigDecimal v){originalPrice=v;}
    public BigDecimal getDiscountPrice(){return discountPrice;} public void setDiscountPrice(BigDecimal v){discountPrice=v;}
    public BigDecimal getDailyEarnings(){return dailyEarnings;} public void setDailyEarnings(BigDecimal v){dailyEarnings=v;}
    public Integer getTotalDays(){return totalDays;} public void setTotalDays(Integer v){totalDays=v;}
    public boolean isActive(){return active;} public void setActive(boolean active){this.active=active;}
    public boolean isArchived(){return archived;} public void setArchived(boolean archived){this.archived=archived;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
