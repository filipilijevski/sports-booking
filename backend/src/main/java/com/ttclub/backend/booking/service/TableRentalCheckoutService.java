package com.ttclub.backend.booking.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.ttclub.backend.booking.dto.TableRentalDtos;
import com.ttclub.backend.booking.model.MembershipPlanType;
import com.ttclub.backend.booking.model.TableRentalPackage;
import com.ttclub.backend.booking.model.TableRentalPurchase;
import com.ttclub.backend.booking.repository.TableRentalPackageRepository;
import com.ttclub.backend.booking.repository.TableRentalPurchaseRepository;
import com.ttclub.backend.booking.repository.UserMembershipRepository;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.TaxService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TableRentalCheckoutService {

    private final TableRentalPackageRepository packs;
    private final TableRentalPurchaseRepository purchases;
    private final UserMembershipRepository userMemberships;
    private final MembershipGuard guard;
    private final TaxService tax;
    private final TableRentalPurchaseOrchestrator orchestrator;

    public TableRentalCheckoutService(TableRentalPackageRepository packs,
                                      TableRentalPurchaseRepository purchases,
                                      UserMembershipRepository userMemberships,
                                      MembershipGuard guard,
                                      TaxService tax,
                                      TableRentalPurchaseOrchestrator orchestrator) {
        this.packs = packs;
        this.purchases = purchases;
        this.userMemberships = userMemberships;
        this.guard = guard;
        this.tax = tax;
        this.orchestrator = orchestrator;
        Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY");
    }

    public TableRentalDtos.QuoteDto quote(Long packageId) {
        TableRentalPackage p = packs.findById(packageId).orElseThrow();
        BigDecimal price = p.getPriceCad();
        BigDecimal taxAmt = tax.calculate(price);
        BigDecimal total = price.add(taxAmt);

        TableRentalDtos.QuoteDto q = new TableRentalDtos.QuoteDto();
        q.priceCad = price.doubleValue();
        q.taxCad   = taxAmt.doubleValue();
        q.totalCad = total.doubleValue();
        q.currency = "CAD";
        return q;
    }

    @Transactional
    public TableRentalDtos.StartResp start(User user, Long packageId) throws StripeException {
        if (user == null) throw new SecurityException("Authentication required.");
        // Must have active INITIAL membership to purchase credits
        guard.ensureInitialMembershipActive(user.getId());

        TableRentalPackage p = packs.findById(packageId).orElseThrow();
        BigDecimal price = p.getPriceCad();
        BigDecimal taxAmt = tax.calculate(price);
        BigDecimal total = price.add(taxAmt);

        TableRentalPurchase trp = new TableRentalPurchase();
        trp.setUser(user);
        trp.setPack(p);
        trp.setPriceCad(price);
        trp.setTaxCad(taxAmt);
        trp.setTotalCad(total);
        trp.setStatus(TableRentalPurchase.Status.PENDING);
        trp = purchases.save(trp);

        long cents = total.movePointRight(2).longValueExact();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(cents)
                .setCurrency("cad")
                .putMetadata("bookingType", "TABLE_CREDITS")
                .putMetadata("bookingId", trp.getId().toString())
                .putMetadata("tableCreditsPackageId", p.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        PaymentIntent pi = PaymentIntent.create(params);
        trp.setStripePaymentIntentId(pi.getId());
        purchases.save(trp);

        TableRentalDtos.StartResp out = new TableRentalDtos.StartResp();
        out.bookingId = trp.getId();
        out.clientSecret = pi.getClientSecret();
        out.priceCad = price.doubleValue();
        out.taxCad = taxAmt.doubleValue();
        out.totalCad = total.doubleValue();
        out.currency = "CAD";
        return out;
    }

    @Transactional
    public void finalizeAfterClientConfirmation(String paymentIntentId, Long bookingId, User user)
            throws StripeException {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("paymentIntentId is required");
        }
        PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
        if (!"succeeded".equalsIgnoreCase(pi.getStatus())) {
            throw new IllegalStateException("PaymentIntent not succeeded (status=" + pi.getStatus() + ")");
        }
        Long fromMeta = null;
        try {
            String s = pi.getMetadata() != null ? pi.getMetadata().get("bookingId") : null;
            if (s != null) fromMeta = Long.valueOf(s);
        } catch (Exception ignore) {}
        Long resolved = (fromMeta != null) ? fromMeta : bookingId;
        orchestrator.onSucceeded(pi.getId(), resolved);
    }
}
