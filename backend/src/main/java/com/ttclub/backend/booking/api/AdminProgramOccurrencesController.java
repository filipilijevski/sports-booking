package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.ProgramOccurrenceDto;
import com.ttclub.backend.booking.service.ProgramOccurrenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/programs")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminProgramOccurrencesController {

    private static final int DEFAULT_ADMIN_RANGE_DAYS = 56; // 8 weeks

    private final ProgramOccurrenceService svc;

    public AdminProgramOccurrencesController(ProgramOccurrenceService svc) {
        this.svc = svc;
    }

    @GetMapping("/{id}/occurrences")
    public List<ProgramOccurrenceDto> listForProgram(
            @PathVariable("id") Long programId,
            @RequestParam(name = "from", required = false) String fromParam,
            @RequestParam(name = "to",   required = false) String toParam) {

        Range r = parseRange(fromParam, toParam);
        return svc.adminFeed(programId, r.from(), r.to());
    }

    /** Soft-cancel future occurrences (skip any with attendance) */
    @PostMapping("/{id}/occurrences/cancel-future")
    public ResponseEntity<CancelResult> cancelFuture(
            @PathVariable("id") Long programId,
            @RequestParam(name = "from", required = false) String fromParam) {

        ZoneId zone = ZoneId.systemDefault();
        Instant from = (fromParam == null || fromParam.isBlank())
                ? LocalDate.now().atStartOfDay(zone).toInstant()
                : parseFlexibleInstant(fromParam, zone, true);

        int cancelled = svc.cancelFuture(programId, from);
        return ResponseEntity.ok(new CancelResult(cancelled));
    }

    /** Cancel future then re-materialize a window for this program */
    @PostMapping("/{id}/occurrences/rebuild")
    public ResponseEntity<RebuildResult> rebuild(
            @PathVariable("id") Long programId,
            @RequestParam(name = "from", required = false) String fromParam,
            @RequestParam(name = "to",   required = false) String toParam) {

        ZoneId zone = ZoneId.systemDefault();
        LocalDate fromDate = (fromParam == null || fromParam.isBlank())
                ? LocalDate.now()
                : parseFlexibleInstant(fromParam, zone, true).atZone(zone).toLocalDate();

        LocalDate toDate = (toParam == null || toParam.isBlank())
                ? fromDate.plusWeeks(12)
                : parseFlexibleInstant(toParam, zone, false).atZone(zone).toLocalDate();

        int inserted = svc.rebuildProgramWindow(programId, fromDate, toDate);
        return ResponseEntity.ok(new RebuildResult(inserted));
    }

    /*  helpers  */

    public static record Range(Instant from, Instant to) {}
    public static record CancelResult(int cancelled) {}
    public static record RebuildResult(int inserted) {}

    private static Range parseRange(String from, String to) {
        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();

        Instant f = (from == null || from.isBlank())
                ? now
                : parseFlexibleInstant(from, zone, true);
        Instant t = (to == null || to.isBlank())
                ? now.plus(Duration.ofDays(DEFAULT_ADMIN_RANGE_DAYS))
                : parseFlexibleInstant(to, zone, false);

        if (!t.isAfter(f)) {
            f = now;
            t = now.plus(Duration.ofDays(1));
        }
        return new Range(f, t);
    }

    private static Instant parseFlexibleInstant(String raw, ZoneId zone, boolean isStart) {
        try { return Instant.parse(raw); } catch (Exception ignore) { }
        try {
            LocalDate d = LocalDate.parse(raw);
            return (isStart ? d.atStartOfDay(zone) : d.plusDays(1).atStartOfDay(zone)).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
