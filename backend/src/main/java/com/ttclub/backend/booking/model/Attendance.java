package com.ttclub.backend.booking.model;

import com.ttclub.backend.model.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_attendance_once",
                columnNames = {"occurrence_id","user_id"}))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* which occurrence */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "occurrence_id")
    private ProgramOccurrence occurrence;

    /* which participant */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /* which enrollment (optional but useful for integrity) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    /* who marked */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marked_by")
    private User markedBy;

    @Column(name = "marked_at", nullable = false)
    private Instant markedAt = Instant.now();

    /* getters/setters */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProgramOccurrence getOccurrence() { return occurrence; }
    public void setOccurrence(ProgramOccurrence occurrence) { this.occurrence = occurrence; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }

    public User getMarkedBy() { return markedBy; }
    public void setMarkedBy(User markedBy) { this.markedBy = markedBy; }

    public Instant getMarkedAt() { return markedAt; }
    public void setMarkedAt(Instant markedAt) { this.markedAt = markedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attendance that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
