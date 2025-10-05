package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    List<MembershipPlan> findAllByActiveTrueOrderByNameAsc();

    @Query("select p from MembershipPlan p where p.type = 'INITIAL' and p.active = true")
    List<MembershipPlan> findActiveInitialPlans();
}
