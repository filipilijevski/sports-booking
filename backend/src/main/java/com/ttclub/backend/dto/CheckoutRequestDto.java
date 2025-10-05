package com.ttclub.backend.dto;

import com.ttclub.backend.model.ShippingMethod;
import jakarta.validation.constraints.NotNull;

/**
 * Payload sent from React checkout page.
 */
public class CheckoutRequestDto {

    @NotNull
    private ShippingAddressDto shippingAddress;

    private ShippingMethod shippingMethod = ShippingMethod.REGULAR;   // safe default

    /** Optional coupon code (case-insensitive). */
    private String couponCode;

    public ShippingAddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressDto a){ this.shippingAddress = a; }

    public ShippingMethod getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(ShippingMethod m) {
        this.shippingMethod = (m == null ? ShippingMethod.REGULAR : m);
    }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
