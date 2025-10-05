package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_templates")
public class ScheduleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* owning coach */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private User coach;

    @Column(nullable = false, length = 9)
    @Enumerated(EnumType.STRING)
    private DayOfWeek weekday;           // MON through SUN

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public Long getId()                   { return id; }
    public User getCoach()               { return coach; }
    public DayOfWeek getWeekday()        { return weekday; }
    public LocalTime getStartTime()      { return startTime; }
    public LocalTime getEndTime()        { return endTime; }
    public Integer getCapacity()         { return capacity; }
    public BigDecimal getPrice()         { return price; }
    public Boolean getActive()           { return active; }

    public void setId(Long id)                   { this.id = id; }
    public void setCoach(User coach)             { this.coach = coach; }
    public void setWeekday(DayOfWeek weekday)    { this.weekday = weekday; }
    public void setStartTime(LocalTime startTime){ this.startTime = startTime; }
    public void setEndTime(LocalTime endTime)    { this.endTime = endTime; }
    public void setCapacity(Integer capacity)    { this.capacity = capacity; }
    public void setPrice(BigDecimal price)       { this.price = price; }
    public void setActive(Boolean active)        { this.active = active; }
}
