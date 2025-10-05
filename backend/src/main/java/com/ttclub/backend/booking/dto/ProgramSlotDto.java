package com.ttclub.backend.booking.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Weekly slot template for a program, with coach assignment */
public class ProgramSlotDto {
    public Long id;
    public Long programId;
    public DayOfWeek weekday;
    public LocalTime startTime;
    public LocalTime endTime;
    public Long coachId;
    /** just convenience for UI rendering :-) */
    public String coachName;
}
