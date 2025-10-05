package com.ttclub.backend.controller;

import com.ttclub.backend.dto.ManualCheckoutRequestDto;
import com.ttclub.backend.dto.OrderDto;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin/Owner manual/in-person checkout from the admin's own cart.
 */
@RestController
@RequestMapping("/api/admin/manual-orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class AdminManualOrderController {

    private final OrderService orders;

    public AdminManualOrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@Valid @RequestBody ManualCheckoutRequestDto body,
                             @AuthenticationPrincipal User admin) {

        return orders.placeOfflineOrderFromAdminCart(admin.getId(), body);
    }
}
