package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.model.*;
import com.ttclub.backend.booking.repository.*;
import com.ttclub.backend.model.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class MembershipPaymentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MembershipPaymentOrchestrator.class);

    private final MembershipPaymentRepository payments;
    private final MembershipPlanRepository plans;
    private final UserMembershipRepository userMemberships;
    private final MembershipGroupRepository groups;
    private final MembershipGroupCounterRepository groupCounters;
    private final MembershipEntitlementRepository entitlements;
    private final EntityManager em;

    public MembershipPaymentOrchestrator(MembershipPaymentRepository payments,
                                         MembershipPlanRepository plans,
                                         UserMembershipRepository userMemberships,
                                         MembershipGroupRepository groups,
                                         MembershipGroupCounterRepository groupCounters,
                                         MembershipEntitlementRepository entitlements,
                                         EntityManager em) {
        this.payments = payments;
        this.plans = plans;
        this.userMemberships = userMemberships;
        this.groups = groups;
        this.groupCounters = groupCounters;
        this.entitlements = entitlements;
        this.em = em;
    }

    @Transactional
    public void onSucceeded(String paymentIntentId, Long bookingIdFromMeta) {
        MembershipPayment mp = null;

        if (bookingIdFromMeta != null) {
            mp = payments.findById(bookingIdFromMeta).orElse(null);
        }
        if (mp == null && paymentIntentId != null) {
            mp = payments.findByStripePaymentIntentId(paymentIntentId).orElse(null);
        }
        if (mp == null) {
            log.warn("MembershipPayment not found for PI {}", paymentIntentId);
            return;
        }
        if (mp.getStatus() == MembershipPayment.Status.SUCCEEDED) {
            return; // idempotent
        }
        mp.setStatus(MembershipPayment.Status.SUCCEEDED);
        payments.save(mp);

        // finalize: create membership(s) and entitlements
        MembershipPlan plan = em.find(MembershipPlan.class, mp.getPlan().getId());
        User buyer = em.find(User.class, mp.getUser().getId());

        if (plan.getHolderKind() == MembershipHolderKind.INDIVIDUAL) {
            createIndividual(plan, buyer, mp.getStartTs(), mp.getEndTs());
        } else {
            createGroup(plan, buyer, mp.getStartTs(), mp.getEndTs());
        }
    }

    @Transactional
    public void onFailedOrCanceled(String paymentIntentId) {
        payments.findByStripePaymentIntentId(paymentIntentId).ifPresent(mp -> {
            if (mp.getStatus() == MembershipPayment.Status.PENDING) {
                mp.setStatus(MembershipPayment.Status.CANCELED);
                payments.save(mp);
            }
        });
    }

    /* helpers */

    private void createIndividual(MembershipPlan plan, User user, Instant start, Instant end) {
        UserMembership um = new UserMembership();
        um.setUser(user);
        um.setPlan(plan);
        um.setStartTs(start);
        um.setEndTs(end);
        um.setActive(true);
        userMemberships.save(um);

        seedEntitlementsIndividual(plan, user, um);
    }

    private void createGroup(MembershipPlan plan, User owner, Instant start, Instant end) {
        MembershipGroup g = new MembershipGroup();
        g.setOwner(owner);
        g.setPlan(plan);
        g.setStartTs(start);
        g.setEndTs(end);
        g.setActive(true);
        g = groups.save(g);

        // Owner also gets a membership row bound to the group
        UserMembership ownerMembership = new UserMembership();
        ownerMembership.setUser(owner);
        ownerMembership.setPlan(plan);
        ownerMembership.setGroup(g);
        ownerMembership.setStartTs(start);
        ownerMembership.setEndTs(end);
        ownerMembership.setActive(true);
        userMemberships.save(ownerMembership);

        seedEntitlementsGroup(plan, owner, g);
    }

    private void seedEntitlementsIndividual(MembershipPlan plan, User user, UserMembership um) {
        List<MembershipEntitlement> list = entitlements.findByPlanId(plan.getId());
        for (MembershipEntitlement e : list) {
            switch (e.getKind()) {
                case TABLE_HOURS -> {
                    TableRentalCredit trc = new TableRentalCredit();
                    trc.setUser(user);
                    trc.setGroup(null);
                    trc.setSourcePlan(plan);
                    trc.setHoursRemaining(e.getAmount());
                    em.persist(trc);
                }
                case PROGRAM_CREDITS, TOURNAMENT_ENTRIES -> {
                    UserMembershipCounter c = new UserMembershipCounter();
                    c.setUserMembership(um);
                    c.setKind(e.getKind());
                    c.setAmountConsumed(BigDecimal.ZERO);
                    em.persist(c);
                }
            }
        }
    }

    private void seedEntitlementsGroup(MembershipPlan plan, User owner, MembershipGroup group) {
        List<MembershipEntitlement> list = entitlements.findByPlanId(plan.getId());
        for (MembershipEntitlement e : list) {
            switch (e.getKind()) {
                case TABLE_HOURS -> {
                    TableRentalCredit trc = new TableRentalCredit();
                    trc.setUser(owner);       // stored on owner but tied to group for pooling
                    trc.setGroup(group);
                    trc.setSourcePlan(plan);
                    trc.setHoursRemaining(e.getAmount());
                    em.persist(trc);
                }
                case PROGRAM_CREDITS, TOURNAMENT_ENTRIES -> {
                    MembershipGroupCounter c = new MembershipGroupCounter();
                    c.setGroup(group);
                    c.setKind(e.getKind());
                    c.setAmountConsumed(BigDecimal.ZERO);
                    groupCounters.save(c);
                }
            }
        }
    }
}
