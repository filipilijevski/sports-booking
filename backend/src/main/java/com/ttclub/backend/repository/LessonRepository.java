package com.ttclub.backend.repository;

import com.ttclub.backend.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    long countByPlayer_Id(Long playerId);          // lessons a user booked
    long countByCoach_Id(Long coachId);        // lessons a coach will give

}
