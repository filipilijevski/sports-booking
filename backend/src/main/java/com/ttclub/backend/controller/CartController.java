package com.ttclub.backend.controller;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService svc;

    public CartController(CartService svc) { this.svc = svc; }

    /* Read */

    @GetMapping
    public CartDto view(@AuthenticationPrincipal User user) {
        return svc.viewCart(user.getEmail());
    }

    /* Create, UPdate, Delete */

    @PostMapping("/items")
    public CartDto add(@RequestBody AddCartItemRequestDto req,
                       @AuthenticationPrincipal User user) {
        return svc.addItem(user.getEmail(), req.getProductId(), req.getQuantity());
    }

    @PatchMapping("/items")
    public CartDto update(@RequestBody UpdateCartItemRequestDto req,
                          @AuthenticationPrincipal User user) {
        return svc.updateItem(user.getEmail(), req.getCartItemId(), req.getQuantity());
    }

    @DeleteMapping("/items")
    public CartDto remove(@RequestBody RemoveCartItemRequestDto req,
                          @AuthenticationPrincipal User user) {
        return svc.removeItem(user.getEmail(), req.getCartItemId());
    }
}
