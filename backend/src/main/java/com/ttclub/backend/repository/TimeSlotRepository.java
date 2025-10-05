package com.ttclub.backend.repository;

import com.ttclub.backend.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeSlotRepository
        extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByCoach_IdAndStartTimeBetween(
            Long coachId, LocalDateTime from, LocalDateTime to);
    boolean existsByCoachIdAndStartTime(Long coachId, LocalDateTime startTime);
}
