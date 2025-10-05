package com.ttclub.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Thin data-carrier for Coupon.
 */
public class CouponDto {

    private Long        id;
    private String      code;
    private BigDecimal  percentOff;   // nullable
    private BigDecimal  amountOff;    // nullable
    private BigDecimal  minSpend;     // nullable
    private Instant     startsAt;
    private Instant     expiresAt;
    private boolean     active;
    private Instant     createdAt;
    private Instant     updatedAt;

    public CouponDto() { }

    public CouponDto(Long id, String code, BigDecimal percentOff, BigDecimal amountOff,
                     BigDecimal minSpend, Instant startsAt, Instant expiresAt,
                     boolean active, Instant createdAt, Instant updatedAt) {
        this.id         = id;
        this.code       = code;
        this.percentOff = percentOff;
        this.amountOff  = amountOff;
        this.minSpend   = minSpend;
        this.startsAt   = startsAt;
        this.expiresAt  = expiresAt;
        this.active     = active;
        this.createdAt  = createdAt;
        this.updatedAt  = updatedAt;
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

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouponDto that)) return false;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "CouponDto[" + id + "," + code + "]"; }
}
