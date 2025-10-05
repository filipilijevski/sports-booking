package com.ttclub.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Purchases placed through the pro-shop.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* buyer (nullable for guest orders) */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    private User user;

    /* money */
    @Column(name = "subtotal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /* discount */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;                 // nullable

    /* shipping method */
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", nullable = false, length = 16)
    private ShippingMethod shippingMethod = ShippingMethod.REGULAR;

    /* status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "order_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    /* Stripe (online) */
    private String stripePaymentIntentId;

    /* Offline (manual) payment marker */
    @Enumerated(EnumType.STRING)
    @Column(name = "offline_payment_method", length = 16)
    private OfflinePaymentMethod offlinePaymentMethod; // null for online

    /* shipping address (embedded) */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName",   column = @Column(name = "shipping_full_name")),
            @AttributeOverride(name = "phone",      column = @Column(name = "shipping_phone")),
            @AttributeOverride(name = "email",      column = @Column(name = "shipping_email")),
            @AttributeOverride(name = "line1",      column = @Column(name = "shipping_line1")),
            @AttributeOverride(name = "line2",      column = @Column(name = "shipping_line2")),
            @AttributeOverride(name = "city",       column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "province",   column = @Column(name = "shipping_province")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code")),
            @AttributeOverride(name = "country",    column = @Column(name = "shipping_country"))
    })
    private ShippingAddress shippingAddress;

    /* audit and version */
    @Column(nullable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();
    @Version                  private Long    version;

    /* items */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /* refund events (readonly, owned by RefundEvent) */
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<RefundEvent> refundEvents = new ArrayList<>();

    @PreUpdate private void touch() { updatedAt = Instant.now(); }

    public Order() {}

    /** Recompute grand total. */
    public void recalculateTotals() {
        totalAmount = subtotalAmount
                .add(shippingFee)
                .add(taxAmount)
                .subtract(discountAmount);
        if (totalAmount.signum() < 0) totalAmount = BigDecimal.ZERO;
    }

    /** @deprecated use getShippingAmount() */
    @Deprecated public BigDecimal getShippingFee()             { return shippingFee; }
    /** @deprecated use setShippingAmount(BigDecimal) */
    @Deprecated public void       setShippingFee(BigDecimal f) { this.shippingFee = f; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal s) { this.subtotalAmount = s; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal t) { this.taxAmount = t; }

    public BigDecimal getShippingAmount() { return shippingFee; }
    public void setShippingAmount(BigDecimal a) { this.shippingFee = a; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal d) { this.discountAmount = d; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal t) { this.totalAmount = t; }

    public Coupon getCoupon() { return coupon; }
    public void   setCoupon(Coupon c) { this.coupon = c; }

    public ShippingMethod getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(ShippingMethod m) { this.shippingMethod = m; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void   setStripePaymentIntentId(String id)  { this.stripePaymentIntentId = id; }

    public OfflinePaymentMethod getOfflinePaymentMethod() { return offlinePaymentMethod; }
    public void setOfflinePaymentMethod(OfflinePaymentMethod offlinePaymentMethod) { this.offlinePaymentMethod = offlinePaymentMethod; }

    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress sa) { this.shippingAddress = sa; }

    public Instant getCreatedAt() { return createdAt; }
    public void    setCreatedAt(Instant t) { this.createdAt = t; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void    setUpdatedAt(Instant t) { this.updatedAt = t; }

    public Long getVersion() { return version; }
    public void setVersion(Long v) { this.version = v; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public List<RefundEvent> getRefundEvents() { return refundEvents; }
    public void setRefundEvents(List<RefundEvent> refundEvents) { this.refundEvents = refundEvents; }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order ord)) return false;
        return Objects.equals(id, ord.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "Order[" + id + "," + status + "," + totalAmount + "]"; }
}
