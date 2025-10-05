package com.ttclub.backend.dto;

import java.math.BigDecimal;

public record ShippingQuoteResponse(
        BigDecimal regular,
        BigDecimal express
) {}
