package com.ttclub.backend.booking.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.ttclub.backend.booking.dto.ProgramInlineDtos.PaymentIntentResp;
import com.ttclub.backend.booking.dto.ProgramInlineDtos.QuoteResp;
import com.ttclub.backend.booking.model.*;
import com.ttclub.backend.booking.model.ProgramEnrollmentMode;
import com.ttclub.backend.booking.repository.ProgramEnrollmentPaymentRepository;
import com.ttclub.backend.booking.repository.ProgramPackageRepository;
import com.ttclub.backend.booking.repository.UserProgramEnrollmentRepository;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.TaxService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProgramEnrollmentInlineService {

    private final ProgramPackageRepository packages;
    private final ProgramEnrollmentPaymentRepository payments;
    private final UserProgramEnrollmentRepository enrollments;
    private final MembershipGuard guard;
    private final TaxService tax;
    private final ProgramEnrollmentPaymentOrchestrator orchestrator;

    public ProgramEnrollmentInlineService(ProgramPackageRepository packages,
                                          ProgramEnrollmentPaymentRepository payments,
                                          UserProgramEnrollmentRepository enrollments,
                                          MembershipGuard guard,
                                          TaxService tax,
                                          ProgramEnrollmentPaymentOrchestrator orchestrator) {
        this.packages = packages;
        this.payments = payments;
        this.enrollments = enrollments;
        this.guard = guard;
        this.tax = tax;
        this.orchestrator = orchestrator;
        Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY");
    }

    /** Public server-side quote for a package. */
    public QuoteResp quote(Long programPackageId) {
        ProgramPackage pkg = packages.findById(programPackageId).orElseThrow();
        BigDecimal price = nonNull(pkg.getPriceCad());
        BigDecimal taxAmount = tax.calculate(price);
        BigDecimal total = price.add(taxAmount);

        QuoteResp q = new QuoteResp();
        q.priceCad = price.doubleValue();
        q.taxCad   = taxAmount.doubleValue();
        q.totalCad = total.doubleValue();
        q.currency = "CAD";
        return q;
    }

    /** Start inline PaymentIntent flow (no redirect). */
    @Transactional
    public PaymentIntentResp startPaymentIntent(User user, Long programPackageId) throws StripeException {
        if (user == null) throw new SecurityException("Authentication required.");

        ProgramPackage pkg = packages.findById(programPackageId).orElseThrow();
        Program program = pkg.getProgram();

        // Business guards
        if (!Boolean.TRUE.equals(program.getActive()) || !pkg.isActive()) {
            throw new IllegalStateException("Program or package is not active.");
        }
        if (program.getEnrollmentMode() == ProgramEnrollmentMode.ADMIN_ONLY) {
            throw new IllegalStateException("This program is by invitation only (admin enrollment required).");
        }

        // Membership prerequisite
        guard.ensureInitialMembershipActive(user.getId());

        // No duplicate active enrollments
        boolean hasActive = enrollments.existsByUser_IdAndProgram_IdAndStatus(
                user.getId(), program.getId(), UserProgramEnrollment.Status.ACTIVE);
        if (hasActive) {
            // Throw a specific, mappable exception for clean 409 responses
            throw new DuplicateEnrollmentException("An active enrollment for this program already exists.");
        }

        // Compute totals
        BigDecimal price = nonNull(pkg.getPriceCad());
        BigDecimal taxAmount = tax.calculate(price);
        BigDecimal total = price.add(taxAmount);

        // Create pending booking row
        ProgramEnrollmentPayment pep = new ProgramEnrollmentPayment();
        pep.setUser(user);
        pep.setProgram(program);
        pep.setProgramPackage(pkg);
        pep.setPriceCad(price);
        pep.setTaxCad(taxAmount);
        pep.setTotalCad(total);
        pep.setStatus(ProgramEnrollmentPayment.Status.PENDING);
        pep = payments.save(pep);

        // Create PaymentIntent
        long cents = total.movePointRight(2).longValueExact();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(cents)
                .setCurrency("cad")
                .putMetadata("bookingType", "ENROLLMENT")
                .putMetadata("bookingId", pep.getId().toString())
                .putMetadata("programId", program.getId().toString())
                .putMetadata("packageId", pkg.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        PaymentIntent pi = PaymentIntent.create(params);
        pep.setStripePaymentIntentId(pi.getId());
        payments.save(pep);

        PaymentIntentResp resp = new PaymentIntentResp(pep.getId(), pi.getClientSecret());
        resp.priceCad = price.doubleValue();
        resp.taxCad   = taxAmount.doubleValue();
        resp.totalCad = total.doubleValue();
        resp.currency = "CAD";
        return resp;
    }

    /**
     * Optional immediate finalize after client-side confirmCardPayment success.<br>
     * Verifies with Stripe that the PI actually succeeded, then calls the same
     * orchestrator used by webhooks. Idempotent if webhook also fires.
     */
    @Transactional
    public void finalizeAfterClientConfirmation(String paymentIntentId, Long bookingId, User user)
            throws StripeException {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("paymentIntentId is required");
        }
        PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
        String status = pi.getStatus();
        if (!"succeeded".equalsIgnoreCase(status)) {
            throw new IllegalStateException("PaymentIntent not succeeded (status=" + status + ")");
        }

        // Prefer Stripe metadata bookingId - fallback to provided
        Long metaBookingId = null;
        try {
            String s = pi.getMetadata() != null ? pi.getMetadata().get("bookingId") : null;
            if (s != null) metaBookingId = Long.valueOf(s);
        } catch (Exception ignored) {}
        Long resolvedBookingId = metaBookingId != null ? metaBookingId : bookingId;

        orchestrator.onSucceeded(pi.getId(), resolvedBookingId);
    }

    private static BigDecimal nonNull(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }
}
