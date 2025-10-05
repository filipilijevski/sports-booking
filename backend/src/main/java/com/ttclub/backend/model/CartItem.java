package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id","product_id"}))
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false) private Integer quantity = 1;

    /** price snapshot (in CAD) taken when item added */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public CartItem() { }

    public Long getId()                      { return id; }
    public void setId(Long id)               { this.id = id; }

    public Cart getCart()                    { return cart; }
    public void setCart(Cart cart)           { this.cart = cart; }

    public Product getProduct()              { return product; }
    public void setProduct(Product product)  { this.product = product; }

    public Integer getQuantity()             { return quantity; }
    public void setQuantity(Integer quantity){ this.quantity = quantity; }

    public BigDecimal getUnitPrice()         { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice){ this.unitPrice = unitPrice; }

    /* computed */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem ci)) return false;
        return Objects.equals(id, ci.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "CartItem[" + id + "]"; }
}
