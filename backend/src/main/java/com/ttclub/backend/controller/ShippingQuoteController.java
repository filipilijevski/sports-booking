package com.ttclub.backend.controller;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.model.*;
import com.ttclub.backend.repository.CartRepository;
import com.ttclub.backend.repository.ProductRepository;
import com.ttclub.backend.service.ShippingRateProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;   // switch
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/shipping")
public class ShippingQuoteController {

    private final CartRepository        carts;
    private final ProductRepository     products;
    private final ShippingRateProvider  rates;

    public ShippingQuoteController(
            CartRepository       carts,
            ProductRepository    products,
            ShippingRateProvider rates) {
        this.carts    = carts;
        this.products = products;
        this.rates    = rates;
    }

    @PostMapping("/quote")
    @Transactional(readOnly = true)          // Spring variant - readOnly OK
    public ShippingQuoteResponse quote(@RequestBody ShippingQuoteRequest req,
                                       @AuthenticationPrincipal User user) {

        if (req.shippingAddress() == null)
            throw new IllegalArgumentException("shippingAddress is required");

        /* Gather cart items - eager fetch prevents lazy errors */
        List<OrderItem> items = new ArrayList<>();

        if (user != null && req.items() == null) {      // logged-in flow
            carts.findByUserIdFetchAll(user.getId()).ifPresent(cart ->
                    cart.getItems().forEach(ci -> {
                        OrderItem oi = new OrderItem();
                        oi.setProduct(ci.getProduct());
                        oi.setQuantity(ci.getQuantity());
                        items.add(oi);
                    })
            );
        } else if (req.items() != null) {               // guest flow
            for (GuestCartItemDto g : req.items()) {
                Product p = products.findById(g.productId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                OrderItem oi = new OrderItem();
                oi.setProduct(p);
                oi.setQuantity(g.quantity());
                items.add(oi);
            }
        }

        if (items.isEmpty())
            throw new IllegalStateException("No cart items available for quote");

        /* Canada Post live rates */
        BigDecimal reg = rates.rateFor(ShippingMethod.REGULAR, items, req.shippingAddress());
        BigDecimal exp = rates.rateFor(ShippingMethod.EXPRESS, items, req.shippingAddress());

        return new ShippingQuoteResponse(reg, exp);
    }
}
