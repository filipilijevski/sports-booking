package com.ttclub.backend.booking.api;

import com.stripe.exception.StripeException;
import com.ttclub.backend.booking.dto.MembershipCheckoutDtos.CheckoutReq;
import com.ttclub.backend.booking.dto.MembershipCheckoutDtos.CheckoutResp;
import com.ttclub.backend.booking.model.MembershipPlan;
import com.ttclub.backend.booking.repository.MembershipPlanRepository;
import com.ttclub.backend.booking.service.MembershipCheckoutService;
import com.ttclub.backend.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/booking/checkout/membership")
public class MembershipCheckoutController {

    private final MembershipCheckoutService svc;
    private final MembershipPlanRepository plans;

    public MembershipCheckoutController(MembershipCheckoutService svc,
                                        MembershipPlanRepository plans) {
        this.svc = svc;
        this.plans = plans;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResp start(@Valid @RequestBody CheckoutReq req,
                              @AuthenticationPrincipal User user) throws StripeException {

        CheckoutResp resp = svc.startCheckout(user, req.planId);

        // add server-side price/tax/total so frontend never guesses
        MembershipPlan plan = plans.findById(req.planId).orElse(null);
        if (plan != null && plan.getPriceCad() != null) {
            BigDecimal price = plan.getPriceCad();
            BigDecimal tax = price.multiply(new BigDecimal("0.13")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = price.add(tax).setScale(2, RoundingMode.HALF_UP);
            resp.priceCad = price.doubleValue();
            resp.taxCad   = tax.doubleValue();
            resp.totalCad = total.doubleValue();
            resp.currency = "CAD";
        }

        return resp;
    }

    /* finalize after client confirmation */

    public record FinalizeReq(String paymentIntentId, Long bookingId) {}

    /**
     * Optional after the client confirms the PaymentIntent successfully,
     * call this endpoint to immediately finalize membership provisioning.
     * We verify with Stripe that the PI is actually "succeeded" before proceeding.
     * Webhook will also call the same orchestrator; both paths are idempotent.
     */
    @PostMapping("/finalize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finalizeAfterClientConfirmation(@RequestBody FinalizeReq req,
                                                @AuthenticationPrincipal User user) throws StripeException {
        svc.finalizeAfterClientConfirmation(req.paymentIntentId(), req.bookingId(), user);
    }
}
