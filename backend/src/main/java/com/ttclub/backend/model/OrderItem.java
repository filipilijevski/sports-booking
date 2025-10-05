/* src/main/java/com/ttclub/backend/model/OrderItem.java */
package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    /* product is nullable so old orders survive product deletion */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    @Column(nullable = false) private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    public OrderItem() { }

    public Long getId()                            { return id; }
    public void setId(Long id)                     { this.id = id; }

    public Order getOrder()                        { return order; }
    public void setOrder(Order order)              { this.order = order; }

    public Product getProduct()                    { return product; }
    public void setProduct(Product product)        { this.product = product; }

    public Integer getQuantity()                   { return quantity; }
    public void setQuantity(Integer quantity)      { this.quantity = quantity; }

    public BigDecimal getUnitPrice()               { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice()              { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice){ this.totalPrice = totalPrice; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem oi)) return false;
        return Objects.equals(id, oi.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "OrderItem[" + id + "]"; }
}
