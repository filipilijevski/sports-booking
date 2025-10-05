package com.ttclub.backend.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.ttclub.backend.model.Order;
import com.ttclub.backend.model.PaymentEvent;
import com.ttclub.backend.repository.OrderRepository;
import com.ttclub.backend.repository.PaymentEventRepository;
import com.ttclub.backend.service.OrderService;
import com.ttclub.backend.booking.service.MembershipPaymentOrchestrator;
import com.ttclub.backend.booking.service.ProgramEnrollmentPaymentOrchestrator;
import com.ttclub.backend.booking.service.TableRentalPurchaseOrchestrator; 
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final String                 signingSecret;
    private final OrderService           ordersSvc;
    private final OrderRepository        orders;
    private final PaymentEventRepository events;
    private final MembershipPaymentOrchestrator membershipOrchestrator;
    private final ProgramEnrollmentPaymentOrchestrator enrollmentOrchestrator;
    private final TableRentalPurchaseOrchestrator tableCreditsOrchestrator; 

    public StripeWebhookController(
            @Value("${stripe.webhook-secret}") String signingSecret,
            OrderService           ordersSvc,
            OrderRepository        orders,
            PaymentEventRepository events,
            MembershipPaymentOrchestrator membershipOrchestrator,
            ProgramEnrollmentPaymentOrchestrator enrollmentOrchestrator,
            TableRentalPurchaseOrchestrator tableCreditsOrchestrator) { 
        this.signingSecret = signingSecret;
        this.ordersSvc     = ordersSvc;
        this.orders        = orders;
        this.events        = events;
        this.membershipOrchestrator = membershipOrchestrator;
        this.enrollmentOrchestrator = enrollmentOrchestrator;
        this.tableCreditsOrchestrator = tableCreditsOrchestrator; 
    }

    @PostMapping({"/api/webhooks/stripe", "/api/stripe/webhook"})
    @Transactional
    public ResponseEntity<String> handle(@RequestHeader("Stripe-Signature") String sigHeader,
                                         @RequestBody String rawBody) {

        Event evt;
        try {
            evt = Webhook.constructEvent(rawBody, sigHeader, signingSecret);
        } catch (SignatureVerificationException ex) {
            log.warn("Stripe signature mismatch - ignoring call");
            return ResponseEntity.status(400).body("bad signature");
        }

        if (!evt.getType().startsWith("payment_intent.")) {
            return ResponseEntity.ok("ignored");
        }

        PaymentIntent pi;
        try {
            Optional<StripeObject> obj = evt.getDataObjectDeserializer().getObject();
            if (obj.isPresent() && obj.get() instanceof PaymentIntent) {
                pi = (PaymentIntent) obj.get();
            } else {
                String rawJson = evt.getDataObjectDeserializer().getRawJson();
                String id = null;
                if (rawJson != null && !rawJson.isBlank()) {
                    JsonElement je = JsonParser.parseString(rawJson);
                    if (je.isJsonObject() && je.getAsJsonObject().has("id")) {
                        id = je.getAsJsonObject().get("id").getAsString();
                    }
                }
                if (id == null || id.isBlank()) {
                    log.error("Unable to extract PaymentIntent id from event {}", evt.getId());
                    return ResponseEntity.status(422).body("cannot extract payment_intent id");
                }
                pi = PaymentIntent.retrieve(id);
            }
        } catch (StripeException ex) {
            log.error("Unable to retrieve PaymentIntent for event {}", evt.getId(), ex);
            return ResponseEntity.status(500).body("stripe fetch failed");
        }

        String bookingType = pi.getMetadata() != null ? pi.getMetadata().get("bookingType") : null;
        String bookingIdRaw = pi.getMetadata() != null ? pi.getMetadata().get("bookingId") : null;
        Long bookingId = null;
        try { if (bookingIdRaw != null) bookingId = Long.valueOf(bookingIdRaw); } catch (NumberFormatException ignore) {}

        /*  Membership flow */
        if ("MEMBERSHIP".equalsIgnoreCase(bookingType)) {
            switch (evt.getType()) {
                case "payment_intent.succeeded"      -> membershipOrchestrator.onSucceeded(pi.getId(), bookingId);
                case "payment_intent.payment_failed",
                     "payment_intent.canceled"       -> membershipOrchestrator.onFailedOrCanceled(pi.getId());
                default -> { /* ignore */ }
            }
            log.info("Processed membership PI: {} (type={})", pi.getId(), evt.getType());
            return ResponseEntity.ok("OK");
        }

        /* Program enrollment flow */
        if ("ENROLLMENT".equalsIgnoreCase(bookingType)) {
            switch (evt.getType()) {
                case "payment_intent.succeeded"      -> enrollmentOrchestrator.onSucceeded(pi.getId(), bookingId);
                case "payment_intent.payment_failed",
                     "payment_intent.canceled"       -> enrollmentOrchestrator.onFailedOrCanceled(pi.getId());
                default -> { /* ignore */ }
            }
            log.info("Processed enrollment PI: {} (type={})", pi.getId(), evt.getType());
            return ResponseEntity.ok("OK");
        }

        /* Table rental credits flow */
        if ("TABLE_CREDITS".equalsIgnoreCase(bookingType)) {
            switch (evt.getType()) {
                case "payment_intent.succeeded"      -> tableCreditsOrchestrator.onSucceeded(pi.getId(), bookingId);
                case "payment_intent.payment_failed",
                     "payment_intent.canceled"       -> tableCreditsOrchestrator.onFailedOrCanceled(pi.getId());
                default -> { /* ignore */ }
            }
            log.info("Processed table-credits PI: {} (type={})", pi.getId(), evt.getType());
            return ResponseEntity.ok("OK");
        }

        /* Legacy shop orders (unchanged) */
        Order order = null;
        String metaId = pi.getMetadata() != null ? pi.getMetadata().get("orderId") : null;
        try { if (metaId != null) order = orders.findById(Long.valueOf(metaId)).orElse(null); } catch (NumberFormatException ignored) { }
        if (order == null) order = orders.findByStripePaymentIntentId(pi.getId()).orElse(null);
        if (order == null) {
            log.warn("No matching order for PaymentIntent {}", pi.getId());
            return ResponseEntity.status(422).body("order not found");
        }

        PaymentEvent pe = new PaymentEvent();
        pe.setOrder(order);
        pe.setProvider("STRIPE");
        pe.setProviderTxnId(pi.getId());
        pe.setAmount(BigDecimal.valueOf(pi.getAmount()).movePointLeft(2));
        pe.setCurrency(pi.getCurrency());
        pe.setStatus(pi.getStatus());
        pe.setEventType(evt.getType());
        pe.setPayloadJson(rawBody);
        events.save(pe);

        switch (evt.getType()) {
            case "payment_intent.succeeded"      -> ordersSvc.syncPaymentStatus(pi.getId(), "succeeded");
            case "payment_intent.payment_failed" -> ordersSvc.syncPaymentStatus(pi.getId(), "payment_failed");
            case "payment_intent.canceled"       -> ordersSvc.syncPaymentStatus(pi.getId(), "canceled");
            default -> { }
        }

        return ResponseEntity.ok("Ok!");
    }
}
