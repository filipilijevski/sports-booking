package com.ttclub.backend.repository;

import com.ttclub.backend.model.GroupLesson;
import com.ttclub.backend.model.GroupLessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupLessonRepository extends JpaRepository<GroupLesson, Long> {

    List<GroupLesson> findByCoachId(Long coachId);
    List<GroupLesson> findByCoachIdAndStatus(Long coachId, GroupLessonStatus status);
}
