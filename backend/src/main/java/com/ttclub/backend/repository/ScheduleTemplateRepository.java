package com.ttclub.backend.repository;

import com.ttclub.backend.model.ScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleTemplateRepository
        extends JpaRepository<ScheduleTemplate, Long> {

    List<ScheduleTemplate> findByCoachId(Long coachId);

    List<ScheduleTemplate> findByCoachIdAndActiveTrue(Long coachId);

    List<ScheduleTemplate> findByActiveTrue();
}
