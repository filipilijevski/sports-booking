package com.ttclub.backend.service;

import com.ttclub.backend.model.Order;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Wraps Stripe PaymentIntent creation.<br>
 * Stripe secret key must be provided as an environment variable
 * <code>STRIPE_SECRET_KEY</code> at runtime.
 */
@Service
public class PaymentService {

    public PaymentService() {
        Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY");
    }

    /**
     * Creates a PaymentIntent for an order (CAD dollars to cents).
     *
     * @throws StripeException if Stripe API call fails
     */
    public PaymentIntent createPaymentIntent(Order order) throws StripeException {

        long amountInCents = order.getTotalAmount()
                .movePointRight(2)            // dollars -> cents
                .longValueExact();            // safe conversion

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("cad")
                .putMetadata("orderId", order.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        return PaymentIntent.create(params);
    }
}
