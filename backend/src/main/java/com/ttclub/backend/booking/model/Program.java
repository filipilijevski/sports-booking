package com.ttclub.backend.booking.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "programs")
public class Program {

    /**
     * Legacy inner enum kept for backward-compatibility with any older call sites.
     * Prefer using the top-level ProgramEnrollmentMode enum across the codebase.
     */
    public enum EnrollmentMode { OPEN, ADMIN_ONLY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    /** OPEN by default - ADMIN_ONLY programs require an admin to enroll the user. */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "enrollment_mode",
            nullable = false,
            columnDefinition = "program_enrollment_mode"
    )
    private ProgramEnrollmentMode enrollmentMode = ProgramEnrollmentMode.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramSlot> slots = new ArrayList<>();

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramPackage> packages = new ArrayList<>();

    @PrePersist
    protected void onInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public boolean isActive() { return Boolean.TRUE.equals(active); }

    /** Primary getter using the top-level enum. */
    public ProgramEnrollmentMode getEnrollmentMode() { return enrollmentMode; }

    /** Primary setter using the top-level enum. */
    public void setEnrollmentMode(ProgramEnrollmentMode enrollmentMode) {
        this.enrollmentMode = enrollmentMode;
    }

    /**
     * Backward-compatibility setter.
     */
    public void setEnrollmentMode(EnrollmentMode mode) {
        this.enrollmentMode = (mode == null) ? null : ProgramEnrollmentMode.valueOf(mode.name());
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ProgramSlot> getSlots() { return slots; }
    public void setSlots(List<ProgramSlot> slots) { this.slots = slots; }

    public List<ProgramPackage> getPackages() { return packages; }
    public void setPackages(List<ProgramPackage> packages) { this.packages = packages; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Program that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
