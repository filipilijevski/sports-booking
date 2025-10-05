package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.model.TableRentalCredit;
import com.ttclub.backend.booking.model.TableRentalPackage;
import com.ttclub.backend.booking.model.TableRentalPurchase;
import com.ttclub.backend.booking.repository.TableRentalPurchaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TableRentalPurchaseOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TableRentalPurchaseOrchestrator.class);

    private final TableRentalPurchaseRepository purchases;
    private final EntityManager em;

    public TableRentalPurchaseOrchestrator(TableRentalPurchaseRepository purchases,
                                           EntityManager em) {
        this.purchases = purchases;
        this.em = em;
    }

    @Transactional
    public void onSucceeded(String paymentIntentId, Long bookingIdFromMeta) {
        TableRentalPurchase trp = null;
        if (bookingIdFromMeta != null) {
            trp = purchases.findById(bookingIdFromMeta).orElse(null);
        }
        if (trp == null && paymentIntentId != null) {
            trp = purchases.findByStripePaymentIntentId(paymentIntentId).orElse(null);
        }
        if (trp == null) {
            log.warn("TableRentalPurchase not found for PI {}", paymentIntentId);
            return;
        }
        if (trp.getStatus() == TableRentalPurchase.Status.SUCCEEDED) {
            return; // idempotent
        }

        trp.setStatus(TableRentalPurchase.Status.SUCCEEDED);
        purchases.save(trp);

        // Provision credits (individual balance)
        TableRentalPackage pack = em.find(TableRentalPackage.class, trp.getPack().getId());
        BigDecimal hours = pack.getHours();

        TableRentalCredit trc = new TableRentalCredit();
        trc.setUser(em.find(com.ttclub.backend.model.User.class, trp.getUser().getId()));
        trc.setGroup(null);
        trc.setSourcePlan(null);
        trc.setHoursRemaining(hours);
        em.persist(trc);
    }

    @Transactional
    public void onFailedOrCanceled(String paymentIntentId) {
        purchases.findByStripePaymentIntentId(paymentIntentId).ifPresent(p -> {
            if (p.getStatus() == TableRentalPurchase.Status.PENDING) {
                p.setStatus(TableRentalPurchase.Status.CANCELED);
                purchases.save(p);
            }
        });
    }
}
