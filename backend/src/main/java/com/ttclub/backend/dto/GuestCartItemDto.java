package com.ttclub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Minimal line-item payload used by guest checkout.
 */
public record GuestCartItemDto(
        @NotNull Long productId,
        @Min(1) int  quantity
) {}
