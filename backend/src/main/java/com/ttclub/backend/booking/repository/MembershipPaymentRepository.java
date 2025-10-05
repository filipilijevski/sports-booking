package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.MembershipPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipPaymentRepository extends JpaRepository<MembershipPayment, Long> {
    Optional<MembershipPayment> findByStripePaymentIntentId(String pi);
}
