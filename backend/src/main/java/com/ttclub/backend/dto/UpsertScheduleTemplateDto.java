package com.ttclub.backend.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Payload used when a coach creates or updates a schedule template.
 */
public record UpsertScheduleTemplateDto(
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        BigDecimal price) { }
