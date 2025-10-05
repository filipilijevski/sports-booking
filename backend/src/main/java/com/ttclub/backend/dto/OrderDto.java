package com.ttclub.backend.dto;

import com.ttclub.backend.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * representation of an Order for API responses.
 */
public class OrderDto {

    private Long id;
    private Long userId;
    private OrderStatus status;

    /* moneyyyy */
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    /* refund summary */
    private BigDecimal refundedAmount;          // sum(refund_events.amount)
    private BigDecimal shippingRefundedAmount;  // sum(refund_events.shipping_amount)
    private Boolean    fullyRefunded;           // refundedAmount >= totalAmount
    private List<RefundDto> refunds;            // detailed list

    /* shipping and payments */
    private String  shippingMethod;
    private String  stripePaymentIntentId;
    private String  offlinePaymentMethod;     

    /* coupon summary */
    private String  couponCode;                

    /* derived origin: ONLINE or IN_PERSON */
    private String  origin;                    

    private ShippingAddressDto shippingAddress;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemDto> items;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal v) { this.subtotalAmount = v; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal v) { this.taxAmount = v; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal v) { this.shippingAmount = v; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal v) { this.discountAmount = v; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }

    public BigDecimal getShippingRefundedAmount() { return shippingRefundedAmount; }
    public void setShippingRefundedAmount(BigDecimal shippingRefundedAmount) { this.shippingRefundedAmount = shippingRefundedAmount; }

    public Boolean getFullyRefunded() { return fullyRefunded; }
    public void setFullyRefunded(Boolean fullyRefunded) { this.fullyRefunded = fullyRefunded; }

    public List<RefundDto> getRefunds() { return refunds; }
    public void setRefunds(List<RefundDto> refunds) { this.refunds = refunds; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String id) { this.stripePaymentIntentId = id; }

    public String getOfflinePaymentMethod() { return offlinePaymentMethod; }
    public void setOfflinePaymentMethod(String offlinePaymentMethod) { this.offlinePaymentMethod = offlinePaymentMethod; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public ShippingAddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressDto a) { this.shippingAddress = a; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}
