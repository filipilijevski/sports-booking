package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {

    @Query("""
           select um
             from UserMembership um
             join um.plan p
            where um.user.id = :userId
              and um.active = true
              and p.type = com.ttclub.backend.booking.model.MembershipPlanType.INITIAL
              and um.startTs <= CURRENT_TIMESTAMP
              and um.endTs   >= CURRENT_TIMESTAMP
           """)
    List<UserMembership> findActiveInitialMembershipsForUser(@Param("userId") Long userId);

    @Query("""
           select count(um) > 0
             from UserMembership um
            where um.user.id = :userId
              and um.plan.id = :planId
              and um.active = true
              and um.startTs <= :now and um.endTs >= :now
           """)
    boolean existsActiveForUserAndPlan(@Param("userId") Long userId,
                                       @Param("planId") Long planId,
                                       @Param("now") Instant now);

    /* count all active memberships for a user (time-windowed, active flag) */
    @Query("""
           select count(um)
             from UserMembership um
            where um.user.id = :userId
              and um.active = true
              and um.startTs <= CURRENT_TIMESTAMP
              and um.endTs   >= CURRENT_TIMESTAMP
           """)
    long countActiveForUser(@Param("userId") Long userId);

    List<UserMembership> findByUser_Id(Long userId);

    long countByGroup_Id(Long groupId);

    List<UserMembership> findByGroup_Id(Long groupId);
    @Query("""
        select distinct g.id
        from UserMembership um
        join um.group g
        where um.user.id = :userId
          and um.active = true
          and um.startTs <= CURRENT_TIMESTAMP
          and um.endTs   >= CURRENT_TIMESTAMP
          and g.active = true
    """)
    List<Long> findActiveGroupIdsForUser(@Param("userId") Long userId);
}
