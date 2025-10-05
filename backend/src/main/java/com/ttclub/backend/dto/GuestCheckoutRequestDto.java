package com.ttclub.backend.dto;

import com.ttclub.backend.model.ShippingMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Payload accepted by /api/guest/checkout.
 */
public record GuestCheckoutRequestDto(

        /* basic contact */
        @Email    @NotBlank String email,
        @NotBlank String firstName,
        @NotBlank String lastName,

        /* shipping address */
        @Valid ShippingAddressDto shippingAddress,

        /* shipping method (nullable => REGULAR) */
        ShippingMethod shippingMethod,

        /* optional coupon code */
        String couponCode,

        /* cart items */
        @Size(min = 1) @Valid List<GuestCartItemDto> items
) {}
