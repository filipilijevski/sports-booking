package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Reusable discount code.
 *
 * Rules:
 *  - Exactly one of percentOff or amountOff is non-null (DB CHECK enforces).<br>
 *  - minSpend is optional (null ⇒ no threshold).<br>
 *  - startsAt / expiresAt window enforced by services (active + window).<br>
 *  - active is an admin toggle; the coupon is "usable" only when:<br>
 *      active == true AND now ∈ [startsAt, expiresAt].<br>
 */
@Entity
@Table(name = "coupons",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Coupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String code;                    // e.g. HELLO15

    @Column(precision = 5, scale = 4)
    private BigDecimal percentOff;          // 0.1500 - 15%, nullable

    @Column(precision = 10, scale = 2)
    private BigDecimal amountOff;           // 10.00 - $10, nullable

    @Column(precision = 10, scale = 2)
    private BigDecimal minSpend;            // nullable - no threshold

    @Column(nullable = false)
    private Instant startsAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = false;         // admin toggle (default off)

    /* audit */
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    private void touch() {
        updatedAt = Instant.now();
    }

    /* helpers */

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isNotStarted() {
        return startsAt != null && startsAt.isAfter(Instant.now());
    }

    /** True only if admin enabled and we are inside the activation window. */
    public boolean isCurrentlyActive() {
        return active && !isExpired() && !isNotStarted();
    }

    /** Check only the minSpend threshold against the supplied spend base. */
    public boolean meetsMinSpend(BigDecimal spendBase) {
        if (minSpend == null || spendBase == null) return true;
        return spendBase.compareTo(minSpend) >= 0;
    }

    /** Compute discount over a given base without eligibility checks. */
    public BigDecimal computeDiscount(BigDecimal base) {
        if (base == null || base.signum() <= 0) return BigDecimal.ZERO;
        if (percentOff != null) {
            return base.multiply(percentOff).setScale(2, RoundingMode.HALF_UP);
        }
        if (amountOff != null) {
            return amountOff.min(base);
        }
        return BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getPercentOff() { return percentOff; }
    public void setPercentOff(BigDecimal percentOff) { this.percentOff = percentOff; }

    public BigDecimal getAmountOff() { return amountOff; }
    public void setAmountOff(BigDecimal amountOff) { this.amountOff = amountOff; }

    public BigDecimal getMinSpend() { return minSpend; }
    public void setMinSpend(BigDecimal minSpend) { this.minSpend = minSpend; }

    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coupon c)) return false;
        return Objects.equals(id, c.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "Coupon[" + id + "," + code + "]"; }
}
