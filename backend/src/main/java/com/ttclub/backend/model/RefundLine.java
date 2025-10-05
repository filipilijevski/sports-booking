package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "refund_lines")
public class RefundLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* parent event */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_event_id")
    private RefundEvent refundEvent;

    /* which order-item */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    /* number of units refunded for this order-item in this event */
    @Column(nullable = false)
    private Integer quantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RefundEvent getRefundEvent() { return refundEvent; }
    public void setRefundEvent(RefundEvent refundEvent) { this.refundEvent = refundEvent; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    /* equality */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefundLine that)) return false;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "RefundLine[" + id + ", item=" + (orderItem == null ? null : orderItem.getId()) +
                ", qty=" + quantity + "]";
    }
}
