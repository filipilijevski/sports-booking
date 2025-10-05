package com.ttclub.backend.repository;

import com.ttclub.backend.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    /**
     * Look-up by the Stripe charge or PaymentIntent id.<br>
     * Column <code>provider_txn_id</code> is UNIQUE, so at most one row exists.
     */
    Optional<PaymentEvent> findByProviderTxnId(String providerTxnId);
}
