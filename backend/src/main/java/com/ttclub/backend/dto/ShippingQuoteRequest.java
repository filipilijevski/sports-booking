package com.ttclub.backend.dto;

import com.ttclub.backend.model.ShippingAddress;

import java.util.List;

/**
 * Request body for POST /api/shipping/quote<br>
 * `items` is only required for guest check-outs.
 */
public record ShippingQuoteRequest(
        ShippingAddress shippingAddress,
        List<GuestCartItemDto> items // nullable for authenticated users
) {}
