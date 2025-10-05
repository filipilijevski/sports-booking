package com.ttclub.backend.controller;

import com.ttclub.backend.dto.OrderDto;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService svc;

    public OrderController(OrderService svc) { this.svc = svc; }

    @GetMapping
    public List<OrderDto> list(@AuthenticationPrincipal User user) {
        return svc.listOwn(user.getId());
    }

    @GetMapping("/{id}")
    public OrderDto detail(@PathVariable Long id,
                           @AuthenticationPrincipal User user) {
        return svc.getOwn(id, user.getId());
    }
}
