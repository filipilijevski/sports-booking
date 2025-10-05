package com.ttclub.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class CartDto {

    private Long id;
    private Long userId;
    private List<CartItemDto> items;
    private BigDecimal subtotal;
    private Integer totalItemCount;
    private Instant updatedAt;

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }
    public Long getUserId()                { return userId; }
    public void setUserId(Long userId)     { this.userId = userId; }
    public List<CartItemDto> getItems()    { return items; }
    public void setItems(List<CartItemDto> i){ this.items = i; }
    public BigDecimal getSubtotal()        { return subtotal; }
    public void setSubtotal(BigDecimal s)  { this.subtotal = s; }
    public Integer getTotalItemCount()     { return totalItemCount; }
    public void setTotalItemCount(Integer c){ this.totalItemCount = c; }
    public Instant getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(Instant t)    { this.updatedAt = t; }
}
