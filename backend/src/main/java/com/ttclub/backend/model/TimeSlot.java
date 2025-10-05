package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_slots",
        uniqueConstraints = @UniqueConstraint(name = "uq_slot",
                columnNames = {"coach_id","start_time"}))
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* owning coach */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private User coach;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Version                        // optimistic locking
    private Integer version = 0;

    public Long getId()                         { return id; }
    public User getCoach()                      { return coach; }
    public LocalDateTime getStartTime()           { return startTime; }
    public LocalDateTime getEndTime()             { return endTime; }
    public Integer getCapacity()                { return capacity; }
    public BigDecimal getPrice()                { return price; }
    public Integer getVersion()                 { return version; }

    public void setId(Long id)                          { this.id = id; }
    public void setCoach(User coach)                    { this.coach = coach; }
    public void setStartTime(LocalDateTime startTime)       { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime)           { this.endTime = endTime; }
    public void setCapacity(Integer capacity)           { this.capacity = capacity; }
    public void setPrice(BigDecimal price)              { this.price = price; }
}
