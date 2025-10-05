/* src/main/java/com/ttclub/backend/controller/GuestCheckoutController.java */
package com.ttclub.backend.controller;

import com.ttclub.backend.config.ShopRateLimitProperties;
import com.ttclub.backend.dto.GuestCheckoutRequestDto;
import com.ttclub.backend.dto.OrderDto;
import com.ttclub.backend.mapper.OrderMapper;
import com.ttclub.backend.mapper.ShippingAddressMapper;
import com.ttclub.backend.model.ShippingAddress;
import com.ttclub.backend.model.ShippingMethod;
import com.ttclub.backend.service.OrderService;
import com.ttclub.backend.service.PaymentService;
import com.ttclub.backend.service.RateLimitService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/guest/checkout")
public class GuestCheckoutController {

    private final OrderService          orders;
    private final PaymentService        payments;
    private final OrderMapper           orderMapper;
    private final ShippingAddressMapper addrMapper;
    private final RateLimitService      rateLimit;
    private final ShopRateLimitProperties rlProps;

    public GuestCheckoutController(OrderService orders,
                                   PaymentService payments,
                                   OrderMapper orderMapper,
                                   ShippingAddressMapper addrMapper,
                                   RateLimitService rateLimit,
                                   ShopRateLimitProperties rlProps) {
        this.orders      = orders;
        this.payments    = payments;
        this.orderMapper = orderMapper;
        this.addrMapper  = addrMapper;
        this.rateLimit   = rateLimit;
        this.rlProps     = rlProps;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> guestCheckout(@Valid @RequestBody GuestCheckoutRequestDto req,
                                             HttpServletRequest http)
            throws StripeException {

        // Guests are not allowed to use coupons
        if (StringUtils.hasText(req.couponCode())) {
            throw new IllegalArgumentException("Coupons are only available for logged-in customers.");
        }

        // Rate-limit PaymentIntent creation (IP+email)
        String ip = clientIp(http);
        rateLimit.check("shop:guest:pi",
                ip + ":" + (req.email() == null ? "" : req.email().trim().toLowerCase()),
                rlProps.getGuestPaymentIntents().getLimit(),
                rlProps.getGuestPaymentIntents().getWindowSec());

        ShippingAddress addr = addrMapper.toEntity(req.shippingAddress());
        ShippingMethod method = (req.shippingMethod() == null)
                ? ShippingMethod.REGULAR : req.shippingMethod();

        var orderEntity = orders.placeGuestOrder(req, addr, method);

        PaymentIntent pi = payments.createPaymentIntent(orderEntity);
        orders.attachPaymentIntent(orderEntity.getId(), pi.getId());

        OrderDto orderDto = orderMapper.toDto(orderEntity);
        return Map.of("order", orderDto, "clientSecret", pi.getClientSecret());
    }

    private static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        return xf != null && !xf.isBlank() ? xf.split(",")[0].trim() : req.getRemoteAddr();
    }
}
