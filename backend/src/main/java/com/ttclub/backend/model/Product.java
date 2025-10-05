package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "products",
        indexes = { @Index(name = "idx_products_sku", columnList = "sku") })
public class Product {

    /* pk */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* FKs */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /* business cols */
    @Column(nullable = false, unique = true, length = 32)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer inventoryQty = 0;

    private String brand;

    @Column(nullable = false)
    private Integer grams;

    @Column(nullable = false)
    private Boolean isActive = Boolean.TRUE;

    /* audit and version */
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    /* relationships */
    @OneToMany(mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    public Product() { }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Category getCategory()                    { return category; }
    public void setCategory(Category category)       { this.category = category; }

    public String getSku()                           { return sku; }
    public void setSku(String sku)                   { this.sku = sku; }

    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }

    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }

    public BigDecimal getPrice()                     { return price; }
    public void setPrice(BigDecimal price)           { this.price = price; }

    public Integer getInventoryQty()                 { return inventoryQty; }
    public void setInventoryQty(Integer inventoryQty){ this.inventoryQty = inventoryQty; }

    public String getBrand()                         { return brand; }
    public void setBrand(String brand)               { this.brand = brand; }

    public Integer getGrams()                        { return grams; }
    public void setGrams(Integer grams)              { this.grams = grams; }

    public Boolean getIsActive()                     { return isActive; }
    public void setIsActive(Boolean active)          { isActive = active; }

    public Instant getCreatedAt()                    { return createdAt; }
    public void setCreatedAt(Instant createdAt)      { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                    { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)      { this.updatedAt = updatedAt; }

    public Long getVersion()                         { return version; }
    public void setVersion(Long version)             { this.version = version; }

    public List<ProductImage> getImages()            { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }

    public void addImage(ProductImage img) {
        images.add(img);
        img.setProduct(this);
    }
    public void removeImage(ProductImage img) {
        images.remove(img);
        img.setProduct(null);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        return Objects.equals(id, p.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "Product[" + id + "," + sku + "," + name + "]";
    }
}
