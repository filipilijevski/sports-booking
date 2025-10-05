package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.ProgramOccurrenceDto;
import com.ttclub.backend.booking.service.ProgramOccurrenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import java.time.*;
import java.util.List;

/**
 * Public calendar feed of program occurrences.
 */
@RestController
@RequestMapping("/api/programs/occurrences")
public class PublicProgramOccurrencesController {

    private final ProgramOccurrenceService svc;

    public PublicProgramOccurrencesController(ProgramOccurrenceService svc) {
        this.svc = svc;
    }

    @GetMapping
    public List<ProgramOccurrenceDto> list(
            @RequestParam(name = "from", required = false) String fromParam,
            @RequestParam(name = "to",   required = false) String toParam) {

        Range r = parseRangeOrDefault(fromParam, toParam, 0, 28); // default 4 weeks
        return svc.publicFeed(r.from, r.to);
    }

    /* helpers  */

    private record Range(Instant from, Instant to) {}

    private static Range parseRangeOrDefault(String from, String to, int defaultFromDays, int defaultToDays) {
        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();

        Instant f = (from == null || from.isBlank())
                ? now.plus(Duration.ofDays(defaultFromDays))
                : parseFlexibleInstant(from, zone, true);
        Instant t = (to == null || to.isBlank())
                ? now.plus(Duration.ofDays(defaultToDays))
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
