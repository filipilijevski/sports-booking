package com.ttclub.backend.service;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.mapper.GroupLessonMapper;
import com.ttclub.backend.model.GroupLesson;
import com.ttclub.backend.model.GroupLessonStatus;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.GroupLessonRepository;
import com.ttclub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class GroupLessonService {

    private final GroupLessonRepository repo;
    private final UserRepository        users;
    private final GroupLessonMapper     mapper;

    public GroupLessonService(GroupLessonRepository repo,
                              UserRepository users,
                              GroupLessonMapper mapper) {
        this.repo   = repo;
        this.users  = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<GroupLessonDto> listForCoach(Long coachId) {
        return mapper.toDto(repo.findByCoachId(coachId));
    }

    public GroupLessonDto create(Long coachId, UpsertGroupLessonDto dto) {

        User coach = users.findById(coachId)
                .orElseThrow(() -> new IllegalArgumentException("Coach not found"));

        GroupLesson gl = mapper.toEntity(dto);
        gl.setCoach(coach);

        repo.save(gl);
        return mapper.toDto(gl);
    }

    public GroupLessonDto update(Long id, UpsertGroupLessonDto dto) {
        GroupLesson gl = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));
        mapper.update(gl, dto);
        return mapper.toDto(gl);
    }

    public void toggleStatus(Long id, boolean active) {
        GroupLesson gl = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));
        gl.setStatus(active ? GroupLessonStatus.ACTIVE : GroupLessonStatus.ARCHIVED);
    }

    public void delete(Long id) { repo.deleteById(id); }
}
