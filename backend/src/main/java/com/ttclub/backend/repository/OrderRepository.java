package com.ttclub.backend.repository;

import com.ttclub.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);
}