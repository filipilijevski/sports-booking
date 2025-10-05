package com.ttclub.backend.booking.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Payload for creating a weekly slot under a program */
public class ProgramSlotCreateReq {
    public DayOfWeek weekday;
    public LocalTime startTime;
    public LocalTime endTime;
    public Long coachId;
}
