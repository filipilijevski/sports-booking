package com.ttclub.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One row per refund attempt created by the system.
 * Amount is POSITIVE in CAD dollars.
 */
@Entity
@Table(name = "refund_events")
public class RefundEvent implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /* PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* links */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    /* business fields */
    @Column(nullable = false, length = 64)
    private String provider;                   // e.g. STRIPE / OFFLINE

    @Column(name = "provider_txn_id", length = 128)
    private String providerTxnId;              // Stripe refund id, etc.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;                 // ALWAYS POSITIVE

    @Column(nullable = false, length = 8)
    private String currency = "cad";

    @Column(length = 32)
    private String status;                     // succeeded / failed

    @Column(length = 64)
    private String reason;                     // requested_by_customer …

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payloadJson;                // raw JSON from provider

    /* shipping markers to prevent double-refunds */
    @Column(name = "includes_shipping", nullable = false)
    private boolean includesShipping = false;

    @Column(name = "shipping_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    /* line-level detail */
    @OneToMany(mappedBy = "refundEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundLine> lines = new ArrayList<>();

    /* audit */
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    private void onInsert() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderTxnId() { return providerTxnId; }
    public void setProviderTxnId(String providerTxnId) { this.providerTxnId = providerTxnId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public boolean isIncludesShipping() { return includesShipping; }
    public void setIncludesShipping(boolean includesShipping) { this.includesShipping = includesShipping; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public List<RefundLine> getLines() { return lines; }
    public void setLines(List<RefundLine> lines) { this.lines = lines; }
    public void addLine(RefundLine rl) { this.lines.add(rl); rl.setRefundEvent(this); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefundEvent re)) return false;
        return Objects.equals(id, re.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "RefundEvent[" +
                "id=" + id +
                ", provider=" + provider +
                ", status=" + status +
                ", amount=" + amount +
                ", includesShipping=" + includesShipping +
                ", shippingAmount=" + shippingAmount +
                ']';
    }
}
