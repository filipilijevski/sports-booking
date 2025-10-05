package com.ttclub.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ProductDto {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer inventoryQty;
    private String brand;
    private Integer grams;

    /** categoryId added for reliable admin-side filtering */
    private Long categoryId;

    private String categoryName;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProductImageDto> images;

    public Long getId()                      { return id; }
    public void setId(Long id)               { this.id = id; }
    public String getSku()                   { return sku; }
    public void setSku(String sku)           { this.sku = sku; }
    public String getName()                  { return name; }
    public void setName(String name)         { this.name = name; }
    public String getDescription()           { return description; }
    public void setDescription(String d)     { this.description = d; }
    public BigDecimal getPrice()             { return price; }
    public void setPrice(BigDecimal price)   { this.price = price; }
    public Integer getInventoryQty()         { return inventoryQty; }
    public void setInventoryQty(Integer q)   { this.inventoryQty = q; }
    public String getBrand()                 { return brand; }
    public void setBrand(String brand)       { this.brand = brand; }
    public Integer getGrams()                { return grams; }
    public void setGrams(Integer g)          { this.grams = g; }

    public Long getCategoryId()              { return categoryId; }
    public void setCategoryId(Long categoryId){ this.categoryId = categoryId; }

    public String getCategoryName()          { return categoryName; }
    public void setCategoryName(String n)    { this.categoryName = n; }
    public Boolean getActive()               { return active; }
    public void setActive(Boolean active)    { this.active = active; }
    public Instant getCreatedAt()            { return createdAt; }
    public void setCreatedAt(Instant t)      { this.createdAt = t; }
    public Instant getUpdatedAt()            { return updatedAt; }
    public void setUpdatedAt(Instant t)      { this.updatedAt = t; }
    public List<ProductImageDto> getImages() { return images; }
    public void setImages(List<ProductImageDto> imgs){ this.images = imgs; }
}
