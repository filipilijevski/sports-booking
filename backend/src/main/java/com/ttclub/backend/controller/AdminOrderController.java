package com.ttclub.backend.controller;

import com.stripe.exception.StripeException;
import com.ttclub.backend.dto.OrderDto;
import com.ttclub.backend.dto.OrderSearchFilter;
import com.ttclub.backend.dto.RefundRequestDto;
import com.ttclub.backend.model.OrderStatus;
import com.ttclub.backend.service.OrderService;
import com.ttclub.backend.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin/Owner-only endpoints for searching orders, changing
 * their status and issuing full / partial refunds.
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class AdminOrderController {

    private final OrderService  orders;
    private final RefundService refunds;

    public AdminOrderController(OrderService orders, RefundService refunds) {
        this.orders  = orders;
        this.refunds = refunds;
    }

    /* SEARCH  (GET /api/admin/orders?status=PAID&orderId=123&email=me@mail)
       Server filters CANCELLED and PENDING_PAYMENT by default */
    @GetMapping
    public List<OrderDto> search(@Valid OrderSearchFilter filter) {
        return orders.search(filter);
    }

    /* PARTIAL or FULL REFUND (POST /{id}/refund) */
    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refund(@PathVariable Long id,
                       @Valid @RequestBody RefundRequestDto dto) throws StripeException {
        refunds.refund(id, dto);   // caps amount and validates remaining balance
    }

    /* STATUS PATCH
       - FULFILLED - mark fulfilled
       - REFUNDED  - full refund (restock + status=REFUNDED)
     */
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable Long id,
                             @RequestParam OrderStatus status) throws StripeException {
        switch (status) {
            case FULFILLED -> orders.markFulfilled(id);
            case REFUNDED  -> refunds.refundOrder(id, "requested_by_customer");
            default        -> throw new IllegalArgumentException("Unsupported transition");
        }
    }
}
