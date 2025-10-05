package com.ttclub.backend.dto;

import com.ttclub.backend.model.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** Optional filters for admin order search. */
public class OrderSearchFilter {

    private Long orderId;
    @Size(max = 128) private String email;
    @Size(max = 128) private String name;
    private OrderStatus status;
    private boolean includePendingPayment = false;

    private Instant dateFrom;
    private Instant dateTo;

    @Min(0) private BigDecimal amountMin;
    @Min(0) private BigDecimal amountMax;

    /** ONLINE or IN_PERSON (case-insensitive). */
    private String origin;

    /** CASH / ETRANSFER / TERMINAL / OTHER (case-insensitive). */
    private String offlinePaymentMethod;

    /* getters/setters */

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public boolean isIncludePendingPayment() { return includePendingPayment; }
    public void setIncludePendingPayment(boolean b){ this.includePendingPayment = b; }

    public Instant getDateFrom() { return dateFrom; }
    public void setDateFrom(Instant dateFrom) { this.dateFrom = dateFrom; }

    public Instant getDateTo() { return dateTo; }
    public void setDateTo(Instant dateTo) { this.dateTo = dateTo; }

    public BigDecimal getAmountMin() { return amountMin; }
    public void setAmountMin(BigDecimal amountMin) { this.amountMin = amountMin; }

    public BigDecimal getAmountMax() { return amountMax; }
    public void setAmountMax(BigDecimal amountMax) { this.amountMax = amountMax; }

    public BigDecimal getMinTotal() { return amountMin; }
    public void setMinTotal(BigDecimal v) { this.amountMin = v; }

    public BigDecimal getMaxTotal() { return amountMax; }
    public void setMaxTotal(BigDecimal v) { this.amountMax = v; }

    public Instant getFrom() { return dateFrom; }
    public void setFrom(Instant t) { this.dateFrom = t; }

    public Instant getTo() { return dateTo; }
    public void setTo(Instant t) { this.dateTo = t; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getOfflinePaymentMethod() { return offlinePaymentMethod; }
    public void setOfflinePaymentMethod(String offlinePaymentMethod) { this.offlinePaymentMethod = offlinePaymentMethod; }

    @Override
    public String toString() {
        return "OrderSearchFilter[" +
                "orderId=" + orderId +
                ", email=" + email +
                ", name=" + name +
                ", status=" + status +
                ", includePendingPayment=" + includePendingPayment +
                ", dateFrom=" + dateFrom +
                ", dateTo=" + dateTo +
                ", amountMin=" + amountMin +
                ", amountMax=" + amountMax +
                ", origin=" + origin +
                ", offlinePaymentMethod=" + offlinePaymentMethod +
                ']';
    }
}
