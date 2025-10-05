package com.ttclub.backend.dto;

import java.math.BigDecimal;

public class OrderItemDto {

    private Long id;
    private ProductDto product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public Long getId()                   { return id; }
    public void setId(Long id)            { this.id = id; }
    public ProductDto getProduct()        { return product; }
    public void setProduct(ProductDto p)  { this.product = p; }
    public Integer getQuantity()          { return quantity; }
    public void setQuantity(Integer q)    { this.quantity = q; }
    public BigDecimal getUnitPrice()      { return unitPrice; }
    public void setUnitPrice(BigDecimal u){ this.unitPrice = u; }
    public BigDecimal getTotalPrice()     { return totalPrice; }
    public void setTotalPrice(BigDecimal t){ this.totalPrice = t; }
}
