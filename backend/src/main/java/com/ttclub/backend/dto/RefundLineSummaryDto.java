package com.ttclub.backend.dto;

/** which order-item and how many units were refunded. */
public class RefundLineSummaryDto {

    private Long orderItemId;
    private int  quantity;

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
