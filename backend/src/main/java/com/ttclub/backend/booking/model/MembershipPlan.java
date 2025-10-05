package com.ttclub.backend.booking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "membership_plans",
        uniqueConstraints = @UniqueConstraint(name = "uq_membership_plan_type_name",
                columnNames = {"type","name"}))
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipPlanType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "holder_kind", nullable = false, length = 16)
    private MembershipHolderKind holderKind = MembershipHolderKind.INDIVIDUAL; // (default)

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "price_cad", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceCad;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MembershipEntitlement> entitlements = new ArrayList<>();

    @PrePersist
    protected void onInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MembershipPlanType getType() { return type; }
    public void setType(MembershipPlanType type) { this.type = type; }

    public MembershipHolderKind getHolderKind() { return holderKind; }
    public void setHolderKind(MembershipHolderKind holderKind) { this.holderKind = holderKind; } 

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPriceCad() { return priceCad; }
    public void setPriceCad(BigDecimal priceCad) { this.priceCad = priceCad; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public boolean isActive() { return Boolean.TRUE.equals(active); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<MembershipEntitlement> getEntitlements() { return entitlements; }
    public void setEntitlements(List<MembershipEntitlement> entitlements) { this.entitlements = entitlements; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipPlan that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
