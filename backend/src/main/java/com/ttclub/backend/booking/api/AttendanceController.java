package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/booking/attendance")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AttendanceController {

    private final AttendanceService svc;

    public AttendanceController(AttendanceService svc) { this.svc = svc; }

    /* DTOs  */

    public record MarkReq(Long occurrenceId, Long userId, Boolean present) { }
    public record EligibleUser(Long userId, String name, String email,
                               Long enrollmentId, Integer sessionsRemaining, boolean present) {}
    public record OccurrenceDto(Long id, Long programId, String title, String start, String end) {}
    public record ListResp(OccurrenceDto occurrence, java.util.List<EligibleUser> users) {}

    /* GET: eligible users for an occurrence OR by program/date */
    @GetMapping
    public ListResp listEligible(@RequestParam(name = "occurrenceId", required = false) Long occurrenceId,
                                 @RequestParam(name = "programId",   required = false) Long programId,
                                 @RequestParam(name = "date",        required = false) String dateIso) {
        LocalDate date = (dateIso == null || dateIso.isBlank()) ? null : LocalDate.parse(dateIso);
        return svc.listEligible(occurrenceId, programId, date);
    }

    /* POST: mark or undo attendance (present=true|false) */
    @PostMapping("/mark")
    public ResponseEntity<Void> mark(@RequestBody MarkReq req, Authentication auth) {
        boolean present = (req.present() == null) || Boolean.TRUE.equals(req.present());
        svc.markAttendance(req.occurrenceId(), req.userId(), present, auth);
        return ResponseEntity.noContent().build();
    }
}
