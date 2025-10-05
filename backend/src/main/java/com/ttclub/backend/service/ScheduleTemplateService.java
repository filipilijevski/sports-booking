package com.ttclub.backend.service;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.mapper.ScheduleTemplateMapper;
import com.ttclub.backend.model.ScheduleTemplate;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.ScheduleTemplateRepository;
import com.ttclub.backend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleTemplateService {

    private final ScheduleTemplateRepository templates;
    private final UserRepository users;
    private final ScheduleTemplateMapper mapper;

    public ScheduleTemplateService(ScheduleTemplateRepository templates,
                                   UserRepository users,
                                   ScheduleTemplateMapper mapper) {
        this.templates = templates;
        this.users     = users;
        this.mapper    = mapper;
    }


    @PreAuthorize("hasRole('COACH') and #coachId == authentication.principal.id or hasRole('ADMIN')")
    public List<ScheduleTemplateDto> listForCoach(Long coachId) {
        return templates.findByCoachId(coachId).stream()
                .map(mapper::toDto).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('COACH') and #coachId == authentication.principal.id or hasRole('ADMIN')")
    public ScheduleTemplateDto create(Long coachId, UpsertScheduleTemplateDto dto) {
        User coach = users.findById(coachId)
                .orElseThrow(() -> new IllegalArgumentException("coach not found"));

        ScheduleTemplate tpl = mapper.toEntity(dto);
        tpl.setCoach(coach);
        return mapper.toDto(templates.save(tpl));
    }

    @Transactional
    @PreAuthorize("hasRole('COACH') and principal.id == @scheduleTemplateRepository.findById(#id).orElseThrow().coach.id or hasRole('ADMIN')")
    public ScheduleTemplateDto update(Long id, UpsertScheduleTemplateDto dto) {
        ScheduleTemplate tpl = templates.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("template not found"));

        mapper.updateEntityFromDto(dto, tpl);
        return mapper.toDto(tpl);        // flush later
    }

    @Transactional
    @PreAuthorize("hasRole('COACH') and principal.id == @scheduleTemplateRepository.findById(#id).orElseThrow().coach.id or hasRole('ADMIN')")
    public void toggleActive(Long id, boolean active) {
        ScheduleTemplate tpl = templates.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("template not found"));
        tpl.setActive(active);
    }

    @Transactional
    @PreAuthorize("hasRole('COACH') and principal.id == @scheduleTemplateRepository.findById(#id).orElseThrow().coach.id or hasRole('ADMIN')")
    public void delete(Long id) { templates.deleteById(id); }
}
