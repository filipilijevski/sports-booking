package com.ttclub.backend.dto;

import com.ttclub.backend.model.GroupLessonStatus;
import com.ttclub.backend.model.LessonStatus;
import java.math.BigDecimal;

/**
 * Read-only view returned to API consumers.
 */
public record GroupLessonDto(
        Long id,
        Long coachId,
        String title,
        String description,
        Integer maxCapacity,
        BigDecimal price,
        GroupLessonStatus status
) { }
