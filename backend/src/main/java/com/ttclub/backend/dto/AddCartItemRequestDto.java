package com.ttclub.backend.dto;

/**
 * Payload for adding a product to the authenticated user’s cart.<br>
 *   POST /api/cart/items
 */
public class AddCartItemRequestDto {

    private Long productId;
    private Integer quantity;

    /* getters / setters */
    public Long getProductId()               { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity()             { return quantity; }
    public void setQuantity(Integer q)       { this.quantity = q; }
}
