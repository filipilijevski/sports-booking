package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.MembershipEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipEntitlementRepository extends JpaRepository<MembershipEntitlement, Long> {
    List<MembershipEntitlement> findByPlanId(Long planId);
}
