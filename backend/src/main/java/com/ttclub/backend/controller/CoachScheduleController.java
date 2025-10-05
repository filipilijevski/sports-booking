package com.ttclub.backend.controller;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.service.ScheduleTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * End-points for coaches to manage recurring availability. Not currently used.
 */
@RestController
@RequestMapping("/api/coach/{coachId}/templates")
public class CoachScheduleController {

    private final ScheduleTemplateService svc;
    public CoachScheduleController(ScheduleTemplateService svc) { this.svc = svc; }

    /* Only coach or admin or owner can call these */

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') or #coachId == authentication.principal.id")
    public List<ScheduleTemplateDto> list(@PathVariable Long coachId) {
        return svc.listForCoach(coachId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') or #coachId == authentication.principal.id")
    public ScheduleTemplateDto create(@PathVariable Long coachId,
                                      @RequestBody UpsertScheduleTemplateDto dto) {
        return svc.create(coachId, dto);
    }

    /* write operations now include {coachId} in the path */

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') or #coachId == authentication.principal.id")
    public ScheduleTemplateDto update(@PathVariable Long coachId,
                                      @PathVariable Long id,
                                      @RequestBody UpsertScheduleTemplateDto dto) {
        return svc.update(id, dto);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') or #coachId == authentication.principal.id")
    public ResponseEntity<Void> toggle(@PathVariable Long coachId,
                                       @PathVariable Long id,
                                       @RequestParam boolean value) {
        svc.toggleActive(id, value);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') or #coachId == authentication.principal.id")
    public ResponseEntity<Void> delete(@PathVariable Long coachId,
                                       @PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }
}
