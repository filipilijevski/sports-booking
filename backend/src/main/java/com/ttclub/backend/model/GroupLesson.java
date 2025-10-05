package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A reusable description of a lesson that can be booked by several players.
 * i.e “Beginner Group A - every Monday 18:00, max 4 people”.
 */
@Entity
@Table(name = "group_lessons")
public class GroupLesson {

    /* cols */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK - users.id : coach who owns the lesson */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id")
    private User coach;

    /** readable title (column in DB renamed from name). */
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    /** Maximum participants (> 1). */
    @Column(name = "capacity", nullable = false)
    private Integer maxCapacity;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private GroupLessonStatus status = GroupLessonStatus.ACTIVE;

    @Version
    private Integer version = 0;

    /* audit helper - optional */
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCoach() { return coach; }
    public void setCoach(User coach) { this.coach = coach; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public GroupLessonStatus getStatus() { return status; }
    public void setStatus(GroupLessonStatus status) { this.status = status; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupLesson other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }

    @Override public String toString() {
        return "GroupLesson{id=%d,title='%s'}".formatted(id, title);
    }
}
