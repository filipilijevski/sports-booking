package com.ttclub.backend.booking.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.ttclub.backend.booking.dto.MembershipCheckoutDtos.CheckoutResp;
import com.ttclub.backend.booking.model.MembershipPayment;
import com.ttclub.backend.booking.model.MembershipPlan;
import com.ttclub.backend.booking.model.MembershipPlanType;
import com.ttclub.backend.booking.repository.MembershipPaymentRepository;
import com.ttclub.backend.booking.repository.MembershipPlanRepository;
import com.ttclub.backend.booking.repository.UserMembershipRepository;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.TaxService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class MembershipCheckoutService {

    private final MembershipPlanRepository plans;
    private final MembershipPaymentRepository payments;
    private final UserMembershipRepository userMemberships;
    private final MembershipGuard guard;
    private final TaxService tax;

    // orchestrator for finalize-after-client-confirmation
    private final MembershipPaymentOrchestrator orchestrator;

    public MembershipCheckoutService(MembershipPlanRepository plans,
                                     MembershipPaymentRepository payments,
                                     UserMembershipRepository userMemberships,
                                     MembershipGuard guard,
                                     TaxService tax,
                                     MembershipPaymentOrchestrator orchestrator) {
        this.plans = plans;
        this.payments = payments;
        this.userMemberships = userMemberships;
        this.guard = guard;
        this.tax = tax;
        this.orchestrator = orchestrator;
        Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY");
    }

    /**
     * Validates business rules, creates a MembershipPayment row, and a Stripe PaymentIntent.
     * Returns (bookingId, clientSecret).
     */
    @Transactional
    public CheckoutResp startCheckout(User user, Long planId) throws StripeException {
        if (user == null) throw new SecurityException("Authentication required.");
        MembershipPlan plan = plans.findById(planId).orElseThrow();

        // Rule 1: Cannot buy INITIAL if already has active INITIAL
        if (plan.getType() == MembershipPlanType.INITIAL && guard.hasActiveInitialMembership(user.getId())) {
            throw new IllegalStateException("You already have an active initial membership.");
        }

        // Rule 2: SPECIAL requires active INITIAL
        if (plan.getType() == MembershipPlanType.SPECIAL && !guard.hasActiveInitialMembership(user.getId())) {
            throw new IllegalStateException("Initial annual club membership is required to purchase this plan.");
        }

        // Rule 3: Cannot hold the same plan twice concurrently
        if (userMemberships.existsActiveForUserAndPlan(user.getId(), plan.getId(), Instant.now())) {
            throw new IllegalStateException("An active membership for this plan already exists.");
        }

        // Compute totals (plan price + tax)
        BigDecimal price = plan.getPriceCad();
        BigDecimal taxAmount = tax.calculate(price);
        BigDecimal total = price.add(taxAmount);

        // Build membership_payment row
        MembershipPayment mp = new MembershipPayment();
        mp.setUser(user);
        mp.setPlan(plan);
        mp.setStartTs(Instant.now());
        mp.setEndTs(Instant.now().plusSeconds((long) plan.getDurationDays() * 86400));
        mp.setPriceCad(price);
        mp.setTaxCad(taxAmount);
        mp.setTotalCad(total);
        mp.setStatus(MembershipPayment.Status.PENDING);
        mp = payments.save(mp);

        long cents = total.movePointRight(2).longValueExact();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(cents)
                .setCurrency("cad")
                .putMetadata("bookingType", "MEMBERSHIP")
                .putMetadata("bookingId", mp.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        PaymentIntent pi = PaymentIntent.create(params);
        mp.setStripePaymentIntentId(pi.getId());
        payments.save(mp);

        return new CheckoutResp(mp.getId(), pi.getClientSecret());
    }

    /**
     * Optional immediate finalize after client-side confirmCardPayment success.<br>
     * We verify with Stripe that the PI actually succeeded (defense-in-depth), then
     * call the same orchestrator used by webhooks. Idempotent safe to call
     * alongside webhooks.
     */
    @Transactional
    public void finalizeAfterClientConfirmation(String paymentIntentId, Long bookingId, User userContext)
            throws StripeException {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("paymentIntentId is required");
        }
        PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
        String status = pi.getStatus();
        if (!"succeeded".equalsIgnoreCase(status)) {
            throw new IllegalStateException("PaymentIntent not succeeded (status=" + status + ")");
        }

        // Prefer Stripe metadata bookingId; fallback to provided
        Long metaBookingId = null;
        try {
            String s = pi.getMetadata() != null ? pi.getMetadata().get("bookingId") : null;
            if (s != null) metaBookingId = Long.valueOf(s);
        } catch (Exception ignored) {}
        Long resolvedBookingId = metaBookingId != null ? metaBookingId : bookingId;

        // Idempotent finalize
        orchestrator.onSucceeded(pi.getId(), resolvedBookingId);
    }
}
