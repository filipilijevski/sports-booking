package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.ProgramEnrollmentPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgramEnrollmentPaymentRepository extends JpaRepository<ProgramEnrollmentPayment, Long> {
    Optional<ProgramEnrollmentPayment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
