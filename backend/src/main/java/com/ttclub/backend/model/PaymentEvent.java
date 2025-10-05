package com.ttclub.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Records every Stripe (or future provider) payment-related webhook
 */
@Entity
@Table(name = "payment_events")
public class PaymentEvent {

    /* pk */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* links */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    /* core fields */
    @Column(nullable = false, length = 64)
    private String provider;               // e.g. "STRIPE"

    @Column(name = "provider_txn_id", length = 128)
    private String providerTxnId;          // charge / refund / PI id

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    private String currency;

    private String status;                 // provider-specific

    private String eventType;              // "payment_intent.succeeded", ...

    /**
     * Raw JSON body exactly as received.
     * <p>
     * <b>Mapping note:</b> we store it in a <code>jsonb</code> column, so we
     * must tell Hibernate this String is JSON - otherwise it binds the value
     * as <i>varchar</i> and Postgres rejects the insert.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public Order getOrder()                    { return order; }
    public void setOrder(Order order)          { this.order = order; }

    public String getProvider()                { return provider; }
    public void setProvider(String provider)   { this.provider = provider; }

    public String getProviderTxnId()           { return providerTxnId; }
    public void setProviderTxnId(String id)    { this.providerTxnId = id; }

    public BigDecimal getAmount()              { return amount; }
    public void setAmount(BigDecimal amount)   { this.amount = amount; }

    public String getCurrency()                { return currency; }
    public void setCurrency(String currency)   { this.currency = currency; }

    public String getStatus()                  { return status; }
    public void setStatus(String status)       { this.status = status; }

    public String getEventType()               { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayloadJson()             { return payloadJson; }
    public void setPayloadJson(String json)    { this.payloadJson = json; }

    public Instant getCreatedAt()              { return createdAt; }
    public void setCreatedAt(Instant t)        { this.createdAt = t; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentEvent pe)) return false;
        return Objects.equals(id, pe.id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "PaymentEvent[" + id + "," + provider + "," + eventType + "]";
    }
}
