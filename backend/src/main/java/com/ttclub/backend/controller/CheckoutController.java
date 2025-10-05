/* src/main/java/com/ttclub/backend/controller/CheckoutController.java */
package com.ttclub.backend.controller;

import com.ttclub.backend.dto.CheckoutRequestDto;
import com.ttclub.backend.dto.OrderDto;
import com.ttclub.backend.mapper.OrderMapper;
import com.ttclub.backend.mapper.ShippingAddressMapper;
import com.ttclub.backend.model.ShippingAddress;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.OrderService;
import com.ttclub.backend.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final OrderService          orders;
    private final PaymentService        payments;
    private final OrderMapper           orderMapper;
    private final ShippingAddressMapper addrMapper;

    public CheckoutController(OrderService orders,
                              PaymentService payments,
                              OrderMapper orderMapper,
                              ShippingAddressMapper addrMapper) {
        this.orders     = orders;
        this.payments   = payments;
        this.orderMapper= orderMapper;
        this.addrMapper = addrMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> checkout(@Valid @RequestBody CheckoutRequestDto req,
                                        @AuthenticationPrincipal User user)
            throws StripeException {

        ShippingAddress sa = addrMapper.toEntity(req.getShippingAddress());

        var orderEntity = orders.placeOrderEntity(
                user.getId(),
                sa,
                req.getShippingMethod());

        orders.applyCoupon(orderEntity, req.getCouponCode(), OrderService.DiscountBase.POST_TAX);

        PaymentIntent pi = payments.createPaymentIntent(orderEntity);
        orders.attachPaymentIntent(orderEntity.getId(), pi.getId());

        OrderDto dto = orderMapper.toDto(orderEntity);
        return Map.of("order", dto, "clientSecret", pi.getClientSecret());
    }
}
