package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_audit")
public class ProductAudit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String action; // Create / Update / Delete / Stock / Image

    private Long productId;     // nullable for safety
    private Long actorUserId;   // admin/owner user id (nullable)

    // snapshot data (denormalized)
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer inventoryQty;
    private String brand;
    private Integer grams;
    private Long categoryId;
    private String categoryName;

    @Column(nullable = false)
    private boolean imageModified = false;

    @Column(columnDefinition = "text")
    private String detailsJson;  // optional diff/extra info

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /* getters/setters */
    public Long getId() { return id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getInventoryQty() { return inventoryQty; }
    public void setInventoryQty(Integer inventoryQty) { this.inventoryQty = inventoryQty; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Integer getGrams() { return grams; }
    public void setGrams(Integer grams) { this.grams = grams; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public boolean isImageModified() { return imageModified; }
    public void setImageModified(boolean imageModified) { this.imageModified = imageModified; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
