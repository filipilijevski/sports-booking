package com.ttclub.backend.model;

/**
 * Matches the PostgreSQL ENUM order_status
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    FULFILLED,
    CANCELLED,
    REFUNDED
}
