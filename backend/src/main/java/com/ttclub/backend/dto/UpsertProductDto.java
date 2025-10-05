package com.ttclub.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Used by admin “Add / Edit product”.  Only NOT-NULL columns are annotated
 * with @NotNull / @NotBlank so Hibernate constraints do not fail.
 */
public class UpsertProductDto {

    @NotBlank  private String sku;
    @NotBlank  private String name;
    @NotNull   private BigDecimal  price;
    @NotNull   private Integer inventoryQty;
    private String description;
    private String brand;
    @NotNull @Positive private Integer grams;
    private Long categoryId;
    private Boolean active = Boolean.TRUE;
    private List<ProductImageDto> images;

    public String getSku()                    { return sku; }
    public void   setSku(String sku)          { this.sku = sku.trim(); }

    public String getName()                   { return name; }
    public void   setName(String name)        { this.name = name.trim(); }

    public BigDecimal getPrice()              { return price; }
    public void   setPrice(BigDecimal price)  { this.price = price; }

    public Integer getInventoryQty()          { return inventoryQty; }
    public void   setInventoryQty(Integer q)  { this.inventoryQty = q; }

    public String getDescription()            { return description; }
    public void   setDescription(String d)    { this.description = d; }

    public String getBrand()                  { return brand; }
    public void   setBrand(String brand)      { this.brand = brand; }

    public Integer getGrams()                 { return grams; }
    public void   setGrams(Integer grams)     { this.grams = grams; }

    public Long getCategoryId()               { return categoryId; }
    public void setCategoryId(Long id)        { this.categoryId = id; }

    public Boolean getActive()                { return active; }
    public void   setActive(Boolean active)   { this.active = active; }

    public List<ProductImageDto> getImages()               { return images; }
    public void setImages(List<ProductImageDto> images)    { this.images = images; }
}
