package com.ttclub.backend.booking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "program_packages", uniqueConstraints = {
        @UniqueConstraint(name = "uq_program_packages_name", columnNames = {"program_id", "name"})
})
public class ProgramPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* owning program */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "sessions_count", nullable = false)
    private Integer sessionsCount;

    @Column(name = "price_cad", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceCad;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    /** Optional UI sorting; may be NULL */
    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

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

    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSessionsCount() { return sessionsCount; }
    public void setSessionsCount(Integer sessionsCount) { this.sessionsCount = sessionsCount; }

    public BigDecimal getPriceCad() { return priceCad; }
    public void setPriceCad(BigDecimal priceCad) { this.priceCad = priceCad; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    /** Convenience boolean view for mappers/JSON - treats null as false. */
    public boolean isActive() { return Boolean.TRUE.equals(active); }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramPackage that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override public int hashCode() { return Objects.hashCode(id); }
}
