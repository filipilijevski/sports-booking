package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 512)
    private String url;

    private String altText;
    private boolean isPrimary;
    private short sortOrder = 0;

    public ProductImage() { }

    public Long getId()                           { return id; }
    public void setId(Long id)                    { this.id = id; }
    public Product getProduct()                   { return product; }
    public void setProduct(Product product)       { this.product = product; }
    public String getUrl()                        { return url; }
    public void setUrl(String url)                { this.url = url; }
    public String getAltText()                    { return altText; }
    public void setAltText(String altText)        { this.altText = altText; }
    public boolean isPrimary()                    { return isPrimary; }
    public void setPrimary(boolean primary)       { isPrimary = primary; }
    public short getSortOrder()                   { return sortOrder; }
    public void setSortOrder(short sortOrder)     { this.sortOrder = sortOrder; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductImage pi)) return false;
        return Objects.equals(id, pi.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "ProductImage[" + id + "," + url + "]"; }
}
