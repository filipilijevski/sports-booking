package com.ttclub.backend.dto;

import java.math.BigDecimal;

/**
 * Payload for create / update requests.
 */
public record UpsertGroupLessonDto(
        String       title,
        String       description,
        Integer      maxCapacity,
        BigDecimal   price
) { }
