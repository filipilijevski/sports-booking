package com.ttclub.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Projection of a refund event suitable for Admin/My Orders. */
public class RefundDto {

    private Long id;

    private String provider;        // STRIPE / OFFLINE
    private String providerTxnId;   // refund id (Stripe) or null

    private BigDecimal amount;      // total refunded in this event (positive)
    private String     currency;    // e.g. "cad"
    private String     status;      // succeeded / failed
    private String     reason;      // requested_by_customer, etc.

    private boolean    includesShipping;
    private BigDecimal shippingAmount;   // portion of shipping refunded in this event

    private Instant createdAt;

    private List<RefundLineSummaryDto> lines;

    /* getters / setters */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderTxnId() { return providerTxnId; }
    public void setProviderTxnId(String providerTxnId) { this.providerTxnId = providerTxnId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isIncludesShipping() { return includesShipping; }
    public void setIncludesShipping(boolean includesShipping) { this.includesShipping = includesShipping; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<RefundLineSummaryDto> getLines() { return lines; }
    public void setLines(List<RefundLineSummaryDto> lines) { this.lines = lines; }
}
