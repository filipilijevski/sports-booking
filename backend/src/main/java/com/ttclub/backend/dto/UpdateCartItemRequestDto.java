package com.ttclub.backend.dto;

/**
 * Payload for changing quantity of an existing cart-item row.
 */
public class UpdateCartItemRequestDto {

    private Long cartItemId;
    private Integer quantity;

    public Long getCartItemId()              { return cartItemId; }
    public void setCartItemId(Long id)       { this.cartItemId = id; }

    public Integer getQuantity()             { return quantity; }
    public void setQuantity(Integer q)       { this.quantity = q; }
}
