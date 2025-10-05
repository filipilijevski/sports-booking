package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.MyMembershipDtos.MyMembershipList;
import com.ttclub.backend.booking.service.MembershipCheckoutService;
import com.ttclub.backend.booking.service.MyMembershipQueryService;
import com.ttclub.backend.model.User;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my/memberships")
public class MyMembershipsController {

    private final MyMembershipQueryService query;
    private final MembershipCheckoutService checkout;

    public MyMembershipsController(MyMembershipQueryService query,
                                   MembershipCheckoutService checkout) {
        this.query = query;
        this.checkout = checkout;
    }

    @GetMapping
    public MyMembershipList list(@AuthenticationPrincipal User user) {
        return query.listForUser(user.getId());
    }

    /**
     * Convenience endpoint for a “Renew” button.
     * Client can also directly call /api/booking/checkout/membership with { planId }.
     */
    @PostMapping("/{planId}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    public Object renew(@PathVariable Long planId,
                        @AuthenticationPrincipal User user) throws StripeException {
        // Delegates to the same membership checkout service
        return checkout.startCheckout(user, planId);
    }
}
