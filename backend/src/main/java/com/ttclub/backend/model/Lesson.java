package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id")
    private User coach;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private User player;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private LessonStatus status = LessonStatus.BOOKED;

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }

    public User getCoach()                    { return coach; }
    public void setCoach(User coach)          { this.coach = coach; }

    public User getPlayer()                   { return player; }
    public void setPlayer(User player)        { this.player = player; }

    public LocalDateTime getStartTime()       { return startTime; }
    public void setStartTime(LocalDateTime s) { this.startTime = s; }

    public LocalDateTime getEndTime()         { return endTime; }
    public void setEndTime(LocalDateTime e)   { this.endTime = e; }

    public LessonStatus getStatus()           { return status; }
    public void setStatus(LessonStatus ls)    { this.status = ls; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lesson other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
