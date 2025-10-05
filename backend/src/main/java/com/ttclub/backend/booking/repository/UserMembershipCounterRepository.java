package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.EntitlementKind;
import com.ttclub.backend.booking.model.UserMembershipCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMembershipCounterRepository extends JpaRepository<UserMembershipCounter, Long> {
    List<UserMembershipCounter> findByUserMembership_Id(Long userMembershipId);
    Optional<UserMembershipCounter> findByUserMembership_IdAndKind(Long userMembershipId, EntitlementKind kind);
}
