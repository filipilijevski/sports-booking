package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.model.*;
import com.ttclub.backend.booking.repository.ProgramEnrollmentPaymentRepository;
import com.ttclub.backend.booking.repository.UserProgramEnrollmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProgramEnrollmentPaymentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ProgramEnrollmentPaymentOrchestrator.class);

    private final ProgramEnrollmentPaymentRepository payments;
    private final UserProgramEnrollmentRepository enrollments;
    private final EntityManager em;

    public ProgramEnrollmentPaymentOrchestrator(ProgramEnrollmentPaymentRepository payments,
                                                UserProgramEnrollmentRepository enrollments,
                                                EntityManager em) {
        this.payments = payments;
        this.enrollments = enrollments;
        this.em = em;
    }

    @Transactional
    public void onSucceeded(String paymentIntentId, Long bookingIdFromMeta) {
        ProgramEnrollmentPayment pep = null;

        if (bookingIdFromMeta != null) {
            pep = payments.findById(bookingIdFromMeta).orElse(null);
        }
        if (pep == null && paymentIntentId != null) {
            pep = payments.findByStripePaymentIntentId(paymentIntentId).orElse(null);
        }
        if (pep == null) {
            log.warn("ProgramEnrollmentPayment not found for PI {}", paymentIntentId);
            return;
        }
        if (pep.getStatus() == ProgramEnrollmentPayment.Status.SUCCEEDED) {
            return; // idempotent
        }

        pep.setStatus(ProgramEnrollmentPayment.Status.SUCCEEDED);
        payments.save(pep);

        // Idempotent guard: if an ACTIVE enrollment already exists, don't create another.
        boolean existsActive = enrollments.existsByUser_IdAndProgram_IdAndStatus(
                pep.getUser().getId(), pep.getProgram().getId(), UserProgramEnrollment.Status.ACTIVE);
        if (existsActive) return;

        // Create enrollment with sessions = package.sessionsCount
        ProgramPackage pkg = em.find(ProgramPackage.class, pep.getProgramPackage().getId());
        UserProgramEnrollment e = new UserProgramEnrollment();
        e.setUser(em.find(com.ttclub.backend.model.User.class, pep.getUser().getId()));
        e.setProgram(em.find(Program.class, pep.getProgram().getId()));
        e.setProgramPackage(pkg);
        e.setStatus(UserProgramEnrollment.Status.ACTIVE);
        e.setSessionsPurchased(pkg.getSessionsCount());
        e.setSessionsRemaining(pkg.getSessionsCount());
        enrollments.save(e);
    }

    @Transactional
    public void onFailedOrCanceled(String paymentIntentId) {
        payments.findByStripePaymentIntentId(paymentIntentId).ifPresent(pep -> {
            if (pep.getStatus() == ProgramEnrollmentPayment.Status.PENDING) {
                pep.setStatus(ProgramEnrollmentPayment.Status.CANCELED);
                payments.save(pep);
            }
        });
    }
}
