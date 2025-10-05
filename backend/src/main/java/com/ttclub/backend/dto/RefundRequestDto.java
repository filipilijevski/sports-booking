package com.ttclub.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Payload for POST /api/admin/orders/{id}/refund
 *
 * amount: optional. null - full refund.<br>
 * reason: mandatory (audit trail).<br>
 * lines : optional line-level restock info for partial refunds.<br>
 * refundShipping: optional (null - default behavior; see RefundService rules)<br>
 */
public class RefundRequestDto {

    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be > 0")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 64)
    private String reason;

    /** Optional: which order-items and quantities are being refunded (for restock). */
    private List<RefundLineDto> lines;

    /** Optional: include shipping in this refund (full or partial). */
    private Boolean refundShipping; 

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<RefundLineDto> getLines() { return lines; }
    public void setLines(List<RefundLineDto> lines) { this.lines = lines; }

    public Boolean getRefundShipping() { return refundShipping; }
    public void setRefundShipping(Boolean refundShipping) { this.refundShipping = refundShipping; }

    @Override
    public String toString() {
        return "RefundRequestDto[amount=" + amount +
                ", reason=" + reason +
                ", lines=" + (lines == null ? 0 : lines.size()) +
                ", refundShipping=" + refundShipping +
                ']';
    }

    /** Row indicating how much of an order-item is to be restocked. */
    public static class RefundLineDto {
        @NotNull private Long orderItemId;
        @Min(1)  private int  quantity;

        public Long getOrderItemId() { return orderItemId; }
        public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
