package com.ttclub.backend.controller;

import com.ttclub.backend.model.Coupon;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.CouponRepository;
import com.ttclub.backend.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
public class CouponUsageController {

    private final CouponRepository coupons;
    private final OrderService orders;

    public CouponUsageController(CouponRepository coupons, OrderService orders) {
        this.coupons = coupons;
        this.orders  = orders;
    }

    /**
     * Logged-in users: early validation of a coupon for the current user.<br>
     * GET /api/coupons/can-use?code=CODE
     */
    @GetMapping("/can-use")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> canUse(@RequestParam("code") String code,
                                      @AuthenticationPrincipal User user) {

        String norm = (code == null ? "" : code.trim().toUpperCase(Locale.ROOT));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", norm);

        Coupon c = coupons.findByCodeIgnoreCase(norm).orElse(null);
        if (c == null) {
            // payload minimal for not found
            resp.put("valid", false);
            return resp;
        }

        boolean active = c.isCurrentlyActive();
        boolean alreadyUsed = orders.hasUserUsedCouponPublic(user.getId(), c.getId());

        resp.put("valid",       active && !alreadyUsed);
        resp.put("active",      active);
        resp.put("alreadyUsed", alreadyUsed);

        resp.put("percentOff", c.getPercentOff());   // BigDecimal or null
        resp.put("amountOff",  c.getAmountOff());    // BigDecimal or null
        resp.put("minSpend",   c.getMinSpend());     // BigDecimal or null
        resp.put("startsAt",   c.getStartsAt());
        resp.put("expiresAt",  c.getExpiresAt());

        return resp;
    }
}
