package com.ttclub.backend.booking.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.ttclub.backend.booking.dto.ProgramCheckoutDtos.CheckoutResp;
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
public class ProgramEnrollmentCheckoutService {

    private final ProgramPackageRepository packages;
    private final ProgramEnrollmentPaymentRepository payments;
    private final UserProgramEnrollmentRepository enrollments;
    private final MembershipGuard guard;
    private final TaxService tax;

    public ProgramEnrollmentCheckoutService(ProgramPackageRepository packages,
                                            ProgramEnrollmentPaymentRepository payments,
                                            UserProgramEnrollmentRepository enrollments,
                                            MembershipGuard guard,
                                            TaxService tax) {
        this.packages = packages;
        this.payments = payments;
        this.enrollments = enrollments;
        this.guard = guard;
        this.tax = tax;
        Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY");
    }

    @Transactional
    public CheckoutResp start(User user, Long programPackageId, String successUrl, String cancelUrl) throws StripeException {
        if (user == null) throw new SecurityException("Authentication required.");
        ProgramPackage pkg = packages.findById(programPackageId).orElseThrow();
        Program program = pkg.getProgram();

        if (!Boolean.TRUE.equals(program.getActive()) || !pkg.isActive()) {
            throw new IllegalStateException("Program or package is not active.");
        }

        if (program.getEnrollmentMode() == ProgramEnrollmentMode.ADMIN_ONLY) {
            throw new IllegalStateException("This program is by invitation only (admin enrollment required).");
        }

        guard.ensureInitialMembershipActive(user.getId());

        boolean hasActive = enrollments.existsByUser_IdAndProgram_IdAndStatus(
                user.getId(), program.getId(), UserProgramEnrollment.Status.ACTIVE);
        if (hasActive) {
            throw new IllegalStateException("An active enrollment for this program already exists.");
        }

        BigDecimal price = pkg.getPriceCad();
        BigDecimal taxAmount = tax.calculate(price);
        BigDecimal total = price.add(taxAmount);

        ProgramEnrollmentPayment pep = new ProgramEnrollmentPayment();
        pep.setUser(user);
        pep.setProgram(program);
        pep.setProgramPackage(pkg);
        pep.setPriceCad(price);
        pep.setTaxCad(taxAmount);
        pep.setTotalCad(total);
        pep = payments.save(pep);

        long cents = total.movePointRight(2).longValueExact();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("cad")
                                                .setUnitAmount(cents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Program: " + program.getTitle() + " • " + pkg.getName())
                                                                .build()
                                                ).build()
                                ).build()
                )
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("bookingType", "ENROLLMENT")
                                .putMetadata("bookingId", String.valueOf(pep.getId()))
                                .putMetadata("programId", String.valueOf(program.getId()))
                                .putMetadata("packageId", String.valueOf(pkg.getId()))
                                .build()
                )
                .build();

        Session session = Session.create(params);
        pep.setStripePaymentIntentId(session.getPaymentIntent());
        payments.save(pep);

        CheckoutResp out = new CheckoutResp(session.getId(), session.getUrl());
        out.priceCad = price.doubleValue();
        out.taxCad = taxAmount.doubleValue();
        out.totalCad = total.doubleValue();
        out.currency = "CAD";
        return out;
    }
}
