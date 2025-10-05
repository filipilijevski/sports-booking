package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.MembershipGroupCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipGroupCounterRepository extends JpaRepository<MembershipGroupCounter, Long> {
    List<MembershipGroupCounter> findByGroup_Id(Long groupId);
    Optional<MembershipGroupCounter> findByGroup_IdAndKind(Long groupId, com.ttclub.backend.booking.model.EntitlementKind kind);
}
