package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByOccurrenceIdAndUserId(Long occurrenceId, Long userId);

    @Query("""
           select distinct a.occurrence.id
             from Attendance a
            where a.occurrence.id in :occurrenceIds
           """)
    List<Long> findAnyByOccurrenceIds(@Param("occurrenceIds") Collection<Long> occurrenceIds);

    /* which users are already marked present for an occurrence */
    @Query("select a.user.id from Attendance a where a.occurrence.id = :occurrenceId")
    List<Long> findUserIdsByOccurrence(@Param("occurrenceId") Long occurrenceId);
}
