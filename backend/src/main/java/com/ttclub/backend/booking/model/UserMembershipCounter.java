package com.ttclub.backend.booking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "user_membership_counters",
        uniqueConstraints = @UniqueConstraint(name = "uq_umc_by_kind",
                columnNames = {"user_membership_id","kind"}))
public class UserMembershipCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_membership_id")
    private UserMembership userMembership;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EntitlementKind kind;

    @Column(name = "amount_consumed", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountConsumed = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserMembership getUserMembership() { return userMembership; }
    public void setUserMembership(UserMembership userMembership) { this.userMembership = userMembership; }

    public EntitlementKind getKind() { return kind; }
    public void setKind(EntitlementKind kind) { this.kind = kind; }

    public BigDecimal getAmountConsumed() { return amountConsumed; }
    public void setAmountConsumed(BigDecimal amountConsumed) { this.amountConsumed = amountConsumed; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserMembershipCounter that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
