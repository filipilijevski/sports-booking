package com.ttclub.backend.booking.model;

import com.ttclub.backend.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "membership_payments",
        indexes = {
                @Index(name = "ix_mp_user", columnList = "user_id"),
                @Index(name = "ix_mp_plan", columnList = "plan_id"),
                @Index(name = "ix_mp_pi",   columnList = "stripe_payment_intent_id")
        })
public class MembershipPayment {

    public enum Status { PENDING, SUCCEEDED, FAILED, CANCELED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private MembershipPlan plan;

    @Column(name = "start_ts", nullable = false)
    private Instant startTs;

    @Column(name = "end_ts", nullable = false)
    private Instant endTs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "price_cad", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceCad;

    @Column(name = "tax_cad", precision = 10, scale = 2, nullable = false)
    private BigDecimal taxCad;

    @Column(name = "total_cad", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalCad;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "cad";

    @Column(name = "stripe_payment_intent_id", length = 100)
    private String stripePaymentIntentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MembershipPlan getPlan() { return plan; }
    public void setPlan(MembershipPlan plan) { this.plan = plan; }

    public Instant getStartTs() { return startTs; }
    public void setStartTs(Instant startTs) { this.startTs = startTs; }

    public Instant getEndTs() { return endTs; }
    public void setEndTs(Instant endTs) { this.endTs = endTs; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getPriceCad() { return priceCad; }
    public void setPriceCad(BigDecimal priceCad) { this.priceCad = priceCad; }

    public BigDecimal getTaxCad() { return taxCad; }
    public void setTaxCad(BigDecimal taxCad) { this.taxCad = taxCad; }

    public BigDecimal getTotalCad() { return totalCad; }
    public void setTotalCad(BigDecimal totalCad) { this.totalCad = totalCad; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipPayment that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
