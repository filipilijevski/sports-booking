package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.TableRentalCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TableRentalCreditRepository extends JpaRepository<TableRentalCredit, Long> {

    @Query("""
           select coalesce(sum(t.hoursRemaining), 0)
             from TableRentalCredit t
            where t.group.id = :groupId
           """)
    BigDecimal sumHoursByGroupId(@Param("groupId") Long groupId);

    @Query("""
           select coalesce(sum(t.hoursRemaining), 0)
             from TableRentalCredit t
            where t.group is null
              and t.user.id = :userId
              and t.sourcePlan.id = :planId
              and t.createdAt >= :start
              and t.createdAt <= :end
           """)
    BigDecimal sumHoursForIndividual(@Param("userId") Long userId,
                                     @Param("planId") Long planId,
                                     @Param("start") Instant start,
                                     @Param("end")   Instant end);

    @Query("""
           select coalesce(sum(t.hoursRemaining), 0)
             from TableRentalCredit t
            where t.group is null
              and t.user.id = :userId
           """)
    BigDecimal sumHoursByUserId(@Param("userId") Long userId);

    /* Lock eligible credit rows (FIFO by created_at) across individual and given groups */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select t
             from TableRentalCredit t
            where t.hoursRemaining > 0
              and (
                    (t.group is null and t.user.id = :userId)
                 or (t.group.id in :groupIds)
              )
            order by t.createdAt asc, t.id asc
           """)
    List<TableRentalCredit> lockEligibleForUser(@Param("userId") Long userId,
                                                @Param("groupIds") Collection<Long> groupIds);
}
