package com.ttclub.backend.booking.model;

import com.ttclub.backend.model.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "user_program_enrollments",
        indexes = {
                @Index(name = "ix_upe_user", columnList = "user_id"),
                @Index(name = "ix_upe_program", columnList = "program_id"),
                @Index(name = "ix_upe_pkg", columnList = "program_package_id")
        }
)
public class UserProgramEnrollment {

    public enum Status { ACTIVE, EXHAUSTED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "program_package_id")
    private ProgramPackage programPackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(name = "sessions_purchased", nullable = false)
    private Integer sessionsPurchased;

    @Column(name = "sessions_remaining", nullable = false)
    private Integer sessionsRemaining;

    @Column(name = "start_ts", nullable = false)
    private Instant startTs = Instant.now();

    @Column(name = "end_ts")
    private Instant endTs;

    @Column(name = "last_attended_at")
    private Instant lastAttendedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /* optimistic locking for concurrent attendance marks */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }

    public ProgramPackage getProgramPackage() { return programPackage; }
    public void setProgramPackage(ProgramPackage programPackage) { this.programPackage = programPackage; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Integer getSessionsPurchased() { return sessionsPurchased; }
    public void setSessionsPurchased(Integer sessionsPurchased) { this.sessionsPurchased = sessionsPurchased; }

    public Integer getSessionsRemaining() { return sessionsRemaining; }
    public void setSessionsRemaining(Integer sessionsRemaining) { this.sessionsRemaining = sessionsRemaining; }

    public Instant getStartTs() { return startTs; }
    public void setStartTs(Instant startTs) { this.startTs = startTs; }

    public Instant getEndTs() { return endTs; }
    public void setEndTs(Instant endTs) { this.endTs = endTs; }

    public Instant getLastAttendedAt() { return lastAttendedAt; }
    public void setLastAttendedAt(Instant lastAttendedAt) { this.lastAttendedAt = lastAttendedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserProgramEnrollment that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
