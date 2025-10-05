package com.ttclub.backend.dto;

/**
 * Payload for deleting a cart-item row.
 *   DELETE /api/cart/items
 */
public class RemoveCartItemRequestDto {

    private Long cartItemId;

    public Long getCartItemId()              { return cartItemId; }
    public void setCartItemId(Long id)       { this.cartItemId = id; }
}
