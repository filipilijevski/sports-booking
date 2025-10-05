package com.ttclub.backend.booking.model;

import com.ttclub.backend.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "table_rental_credits",
        indexes = {
                @Index(name = "ix_trc_user", columnList = "user_id"),
                @Index(name = "ix_trc_group", columnList = "group_id") 
        })
public class TableRentalCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* owner (still required - group credits are pooled but associated to owner as well) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /* optional group for shared credits */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private MembershipGroup group;

    @Column(name = "hours_remaining", nullable = false, precision = 6, scale = 2)
    private BigDecimal hoursRemaining;

    /* optional link to the membership plan that granted this credit */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_plan_id")
    private MembershipPlan sourcePlan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /* getters/setters */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MembershipGroup getGroup() { return group; }
    public void setGroup(MembershipGroup group) { this.group = group; }

    public BigDecimal getHoursRemaining() { return hoursRemaining; }
    public void setHoursRemaining(BigDecimal hoursRemaining) { this.hoursRemaining = hoursRemaining; }

    public MembershipPlan getSourcePlan() { return sourcePlan; }
    public void setSourcePlan(MembershipPlan sourcePlan) { this.sourcePlan = sourcePlan; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableRentalCredit that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
