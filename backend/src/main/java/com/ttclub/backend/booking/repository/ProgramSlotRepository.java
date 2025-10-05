package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.ProgramSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;

public interface ProgramSlotRepository extends JpaRepository<ProgramSlot, Long> {
    List<ProgramSlot> findByProgramIdOrderByWeekdayAscStartTimeAsc(Long programId);
    List<ProgramSlot> findByProgramIdAndWeekdayOrderByStartTimeAsc(Long programId, DayOfWeek weekday);

    // Bulk load for multiple programs (avoids N+1 hopefully)
    List<ProgramSlot> findByProgram_IdInOrderByWeekdayAscStartTimeAsc(Collection<Long> programIds);
}
