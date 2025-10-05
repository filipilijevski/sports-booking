package com.ttclub.backend.controller;

import com.ttclub.backend.dto.CouponDto;
import com.ttclub.backend.model.Coupon;
import com.ttclub.backend.repository.CouponRepository;
import com.ttclub.backend.service.CouponService;
import com.ttclub.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN') || hasRole('OWNER')")
public class AdminCouponController {

    private final CouponService svc;
    private final CouponRepository coupons;
    private final OrderService orders;

    public AdminCouponController(CouponService svc,
                                 CouponRepository coupons,
                                 OrderService orders) {
        this.svc = svc;
        this.coupons = coupons;
        this.orders  = orders;
    }

    @GetMapping
    public List<CouponDto> list() { return svc.listAll(); }

    @GetMapping("/{id}")
    public CouponDto get(@PathVariable Long id) { return svc.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponDto create(@RequestBody CouponDto dto) { return svc.create(dto); }

    @PutMapping("/{id}")
    public CouponDto update(@PathVariable Long id, @RequestBody CouponDto dto) {
        return svc.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { svc.delete(id); }

    /**
     * Admin-only validation on behalf of a selected user.<br>
     * GET /api/admin/coupons/can-use?code=CODE&userId=123
     */
    @GetMapping("/can-use")
    public Map<String, Object> canUseForUser(@RequestParam("code") String code,
                                             @RequestParam("userId") Long userId) {

        String norm = (code == null ? "" : code.trim().toUpperCase(Locale.ROOT));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", norm);

        Coupon c = coupons.findByCodeIgnoreCase(norm).orElse(null);
        if (c == null) {
            resp.put("valid", false);
            return resp;
        }

        boolean active = c.isCurrentlyActive();
        boolean alreadyUsed = orders.hasUserUsedCouponPublic(userId, c.getId());

        resp.put("valid",       active && !alreadyUsed);
        resp.put("active",      active);
        resp.put("alreadyUsed", alreadyUsed);

        // Nullable fields are allowed here, and the frontend already accounts for nulls
        resp.put("percentOff", c.getPercentOff());
        resp.put("amountOff",  c.getAmountOff());
        resp.put("minSpend",   c.getMinSpend());
        resp.put("startsAt",   c.getStartsAt());
        resp.put("expiresAt",  c.getExpiresAt());

        return resp;
    }
}
