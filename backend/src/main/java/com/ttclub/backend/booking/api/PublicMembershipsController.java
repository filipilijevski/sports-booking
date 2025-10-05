package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.MembershipPlanDto;
import com.ttclub.backend.booking.model.MembershipPlan;
import com.ttclub.backend.booking.repository.MembershipPlanRepository;
import com.ttclub.backend.booking.service.AdminMembershipService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class PublicMembershipsController {

    private final AdminMembershipService membershipService;
    private final MembershipPlanRepository plans;

    public PublicMembershipsController(AdminMembershipService membershipService,
                                       MembershipPlanRepository plans) {
        this.membershipService = membershipService;
        this.plans = plans;
    }

    @GetMapping("/plans")
    public List<MembershipPlanDto> listActive() {
        return membershipService.listPublicPlans();
    }

    /** Lightweight server-side quote so the UI can display authoritative tax and total */
    @GetMapping("/plans/{id}/quote")
    public QuoteResp quote(@PathVariable Long id) {
        MembershipPlan p = plans.findById(id).orElseThrow();
        BigDecimal price = p.getPriceCad() == null ? BigDecimal.ZERO : p.getPriceCad();

        // HST 13% (aligns with current Pro-Shop). If you need province-based tax,
        // plug in your tax service here and keep the client unchanged.
        BigDecimal tax = price.multiply(new BigDecimal("0.13")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = price.add(tax).setScale(2, RoundingMode.HALF_UP);

        QuoteResp q = new QuoteResp();
        q.priceCad = price.doubleValue();
        q.taxCad   = tax.doubleValue();
        q.totalCad = total.doubleValue();
        q.currency = "CAD";
        return q;
    }

    public static class QuoteResp {
        public Double priceCad;
        public Double taxCad;
        public Double totalCad;
        public String currency;
    }
}
