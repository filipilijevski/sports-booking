package com.ttclub.backend.model;

/**
 * Lifecycle states for a class template that a coach offers.
 * <br>
 * • {@code ACTIVE}   - visible in the “menu” and can be scheduled<br>
 * • {@code ARCHIVED} - retired; kept only for historical data
 */
public enum GroupLessonStatus {
    ACTIVE,
    ARCHIVED
}
