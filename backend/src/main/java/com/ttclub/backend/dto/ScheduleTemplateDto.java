package com.ttclub.backend.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Read‑only projection of a recurring coach availability block.<br>
 * Not currently used as we have the booking package, but could be useful in future?
 */
public record ScheduleTemplateDto(
        Long id,
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        BigDecimal price,
        Boolean active) { }
