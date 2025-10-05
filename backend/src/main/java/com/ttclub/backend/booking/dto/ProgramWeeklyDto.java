package com.ttclub.backend.booking.dto;

import java.time.DayOfWeek;
import java.util.List;

/** One weekday entry with one or more time ranges (possibly different coaches) */
public class ProgramWeeklyDto {
    public DayOfWeek weekday;
    public List<TimeRange> times;
}
