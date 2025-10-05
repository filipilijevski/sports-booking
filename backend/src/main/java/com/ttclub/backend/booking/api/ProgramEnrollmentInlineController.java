package com.ttclub.backend.booking.api;

import com.stripe.exception.StripeException;
import com.ttclub.backend.booking.dto.ProgramInlineDtos.PaymentIntentResp;
import com.ttclub.backend.booking.dto.ProgramInlineDtos.QuoteResp;
import com.ttclub.backend.booking.service.ProgramEnrollmentInlineService;
import com.ttclub.backend.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Inline PaymentIntent flow for program enrollment (no hosted Checkout redirect).
 * Endpoints:<br>
 *  GET  /api/programs/packages/{id}/quote<br>
 *  POST /api/programs/packages/{id}/payment-intent<br>
 *  POST /api/programs/packages/finalize           (optional; mirrors memberships)<br>
 */
@RestController
@RequestMapping("/api/programs/packages")
public class ProgramEnrollmentInlineController {

    private final ProgramEnrollmentInlineService svc;

    public ProgramEnrollmentInlineController(ProgramEnrollmentInlineService svc) {
        this.svc = svc;
    }

    /** Lightweight server-side quote so the UI can display authoritative tax & total. */
    @GetMapping("/{id}/quote")
    public QuoteResp quote(@PathVariable("id") Long packageId) {
        return svc.quote(packageId);
    }

    /**
     * Create a PaymentIntent and booking row for the given program package.
     * Returns client_secret + booking id + server-side price/tax/total.
     */
    @PostMapping("/{id}/payment-intent")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentIntentResp createPaymentIntent(@PathVariable("id") Long packageId,
                                                 @AuthenticationPrincipal User user) throws StripeException {
        return svc.startPaymentIntent(user, packageId);
    }

    /* Optional: finalize after client confirmation  */

    public record FinalizeReq(String paymentIntentId, Long bookingId) {}

    /**
     * Optional: after client confirms the PaymentIntent successfully,
     * call this to immediately finalize enrollment. We verify with Stripe the PI is
     * actually "succeeded" before proceeding. Webhooks should also finalize.
     */
    @PostMapping("/finalize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finalizeAfterClientConfirmation(@RequestBody FinalizeReq req,
                                                @AuthenticationPrincipal User user) throws StripeException {
        svc.finalizeAfterClientConfirmation(req.paymentIntentId(), req.bookingId(), user);
    }
}
