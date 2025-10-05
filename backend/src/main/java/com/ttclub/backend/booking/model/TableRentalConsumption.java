package com.ttclub.backend.booking.model;

import com.ttclub.backend.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "table_rental_consumptions",
        indexes = {
                @Index(name = "ix_trcons_user", columnList = "user_id"),
                @Index(name = "ix_trcons_trc",  columnList = "trc_id"),
                @Index(name = "ix_trcons_admin", columnList = "consumed_by")
        })
public class TableRentalConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The client whose balance decreased */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** The admin who performed the action */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "consumed_by")
    private User consumedBy;

    /** Which TRC row was decremented (for audit) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trc_id")
    private TableRentalCredit trc;

    /** Optional - if consumption pulled from a group pool */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private MembershipGroup group;

    @Column(name = "amount_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal amountHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getConsumedBy() { return consumedBy; }
    public void setConsumedBy(User consumedBy) { this.consumedBy = consumedBy; }
    public TableRentalCredit getTrc() { return trc; }
    public void setTrc(TableRentalCredit trc) { this.trc = trc; }
    public MembershipGroup getGroup() { return group; }
    public void setGroup(MembershipGroup group) { this.group = group; }
    public BigDecimal getAmountHours() { return amountHours; }
    public void setAmountHours(BigDecimal amountHours) { this.amountHours = amountHours; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableRentalConsumption that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
