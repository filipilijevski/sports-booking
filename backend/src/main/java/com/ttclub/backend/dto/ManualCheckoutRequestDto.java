package com.ttclub.backend.dto;

import com.ttclub.backend.model.ShippingMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Admin-only: finalize a manual/in-person order from the admin's cart.<br>
 * If clientUserId is provided, it wins. Otherwise, a user is looked up by email
 * or created (CLIENT role) with the provided fullName/phone.
 */
public class ManualCheckoutRequestDto {

    private Long clientUserId;

    @Size(max = 128)
    private String clientFullName; // mandatory when clientUserId is null

    @Email
    private String clientEmail; // mandatory when clientUserId is null

    @Size(max = 32)
    private String clientPhone; // mandatory when clientUserId is null

    @Valid
    private ShippingAddressDto shippingAddress; // optional

    private ShippingMethod shippingMethod = ShippingMethod.REGULAR;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal shippingFee; // optional override (null - compute or 0)

    @NotBlank
    private String paymentMethod; // CASH / ETRANSFER / TERMINAL / OTHER

    private String couponCode; // optional

    public Long getClientUserId() { return clientUserId; }
    public void setClientUserId(Long clientUserId) { this.clientUserId = clientUserId; }

    public String getClientFullName() { return clientFullName; }
    public void setClientFullName(String clientFullName) { this.clientFullName = clientFullName; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public ShippingAddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public ShippingMethod getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod == null ? ShippingMethod.REGULAR : shippingMethod;
    }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
