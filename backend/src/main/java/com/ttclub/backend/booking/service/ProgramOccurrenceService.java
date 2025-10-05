package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.dto.ProgramOccurrenceDto;
import com.ttclub.backend.booking.model.Program;
import com.ttclub.backend.booking.model.ProgramSlot;
import com.ttclub.backend.booking.repository.AttendanceRepository;
import com.ttclub.backend.booking.repository.ProgramOccurrenceRepository;
import com.ttclub.backend.booking.repository.ProgramRepository;
import com.ttclub.backend.booking.repository.ProgramSlotRepository;
import com.ttclub.backend.model.User;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds and reads program occurrences (calendar view).
 * - Idempotence via unique (program_id, start_ts).
 * - coach_id is NOT NULL; slots lacking a coach are skipped.
 * - Reconciliation: if an existing occurrence with same (program, start_ts) exists,
 *   update coach and endTs; if it was cancelled, un-cancel it.
 * - Rescheduling (weekday/start change) remains append-only by default; use cancel/rebuild admin endpoints.
 */
@Service
public class ProgramOccurrenceService {

    private static final Logger log = LoggerFactory.getLogger(ProgramOccurrenceService.class);
    private static final int DEFAULT_WEEKS = 12;

    private final ProgramRepository programs;
    private final ProgramSlotRepository slots;
    private final ProgramOccurrenceRepository occurrences;
    private final AttendanceRepository attendance;

    public ProgramOccurrenceService(ProgramRepository programs,
                                    ProgramSlotRepository slots,
                                    ProgramOccurrenceRepository occurrences,
                                    AttendanceRepository attendance) {
        this.programs = programs;
        this.slots = slots;
        this.occurrences = occurrences;
        this.attendance = attendance;
    }

    /* Materialise */

    @Transactional
    public int materializeUpcoming(int weeksAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = startDate.plusWeeks(Math.max(1, weeksAhead));
        return materializeRange(startDate, endDate);
    }

    @Transactional
    public int materializeRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) return 0;

        ZoneId zone = ZoneId.systemDefault();
        Instant from = startDate.atStartOfDay(zone).toInstant();
        Instant to   = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        // Active programs only
        List<Program> activePrograms = programs.findAllByActiveTrueOrderByTitleAsc();
        if (activePrograms.isEmpty()) return 0;

        // Map programId -> slots
        Map<Long, List<ProgramSlot>> slotsByProgram = new HashMap<>();
        for (Program p : activePrograms) {
            List<ProgramSlot> list = slots.findByProgramIdOrderByWeekdayAscStartTimeAsc(p.getId());
            if (!list.isEmpty()) slotsByProgram.put(p.getId(), list);
        }
        if (slotsByProgram.isEmpty()) return 0;

        // Load existing occurrences (including cancelled) within window
        var existingList = occurrences.findKeysInRange(from, to);
        Map<String, com.ttclub.backend.booking.model.ProgramOccurrence> existingMap = existingList.stream()
                .collect(Collectors.toMap(
                        o -> key(o.getProgram() != null ? o.getProgram().getId() : null, o.getStartTs()),
                        o -> o,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<com.ttclub.backend.booking.model.ProgramOccurrence> toInsert = new ArrayList<>();
        List<com.ttclub.backend.booking.model.ProgramOccurrence> toUpdate = new ArrayList<>();
        int skippedNoCoach = 0;

        for (Program p : activePrograms) {
            List<ProgramSlot> programSlots = slotsByProgram.get(p.getId());
            if (programSlots == null) continue;

            skippedNoCoach += processProgramWindow(p, programSlots, startDate, endDate, zone, existingMap, toInsert, toUpdate);
        }

        if (!toInsert.isEmpty()) occurrences.saveAll(toInsert);
        if (!toUpdate.isEmpty()) occurrences.saveAll(toUpdate);

        int created = toInsert.size();
        int updated = toUpdate.size();
        if (skippedNoCoach > 0) {
            log.warn("Materializer: skipped {} slot occurrences due to missing coach (coach_id is required).", skippedNoCoach);
        }
        log.info("Materializer (all): {} inserted, {} updated ({} .. {}).", created, updated, from, to);
        return created;
    }

    /** Materialize a single program now (used after slot add/update). */
    @Transactional
    public int materializeForProgram(Long programId, int weeksAhead) {
        Program p = programs.findById(programId).orElseThrow();
        if (!p.isActive()) return 0;

        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = startDate.plusWeeks(Math.max(1, weeksAhead));

        ZoneId zone = ZoneId.systemDefault();
        Instant from = startDate.atStartOfDay(zone).toInstant();
        Instant to   = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        List<ProgramSlot> programSlots = slots.findByProgramIdOrderByWeekdayAscStartTimeAsc(programId);
        if (programSlots.isEmpty()) return 0;

        var existingList = occurrences.findAllForProgramInRange(programId, from, to);
        Map<String, com.ttclub.backend.booking.model.ProgramOccurrence> existingMap = existingList.stream()
                .collect(Collectors.toMap(
                        o -> key(programId, o.getStartTs()),
                        o -> o,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<com.ttclub.backend.booking.model.ProgramOccurrence> toInsert = new ArrayList<>();
        List<com.ttclub.backend.booking.model.ProgramOccurrence> toUpdate = new ArrayList<>();

        int skippedNoCoach = processProgramWindow(p, programSlots, startDate, endDate, zone, existingMap, toInsert, toUpdate);

        if (!toInsert.isEmpty()) occurrences.saveAll(toInsert);
        if (!toUpdate.isEmpty()) occurrences.saveAll(toUpdate);

        int created = toInsert.size();
        int updated = toUpdate.size();
        if (skippedNoCoach > 0) {
            log.warn("Materializer (program {}): skipped {} due to missing coach.", programId, skippedNoCoach);
        }
        log.info("Materializer (program {}): {} inserted, {} updated ({} .. {}).", programId, created, updated, from, to);
        return created;
    }

    private int processProgramWindow(Program p,
                                     List<ProgramSlot> programSlots,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     ZoneId zone,
                                     Map<String, com.ttclub.backend.booking.model.ProgramOccurrence> existingMap,
                                     List<com.ttclub.backend.booking.model.ProgramOccurrence> toInsert,
                                     List<com.ttclub.backend.booking.model.ProgramOccurrence> toUpdate) {

        int skippedNoCoach = 0;

        for (ProgramSlot s : programSlots) {
            if (s.getCoach() == null) {
                skippedNoCoach++;
                continue; // coach_id NOT NULL in occurrences
            }
            DayOfWeek target = s.getWeekday();
            LocalDate d = startDate.with(TemporalAdjusters.nextOrSame(target));

            while (!d.isAfter(endDate)) {
                ZonedDateTime zStart = ZonedDateTime.of(d, s.getStartTime(), zone);
                ZonedDateTime zEnd   = ZonedDateTime.of(d, s.getEndTime(),   zone);
                if (!zEnd.isAfter(zStart)) {
                    d = d.plusWeeks(1);
                    continue;
                }

                Instant startTs = zStart.toInstant();
                Instant endTs   = zEnd.toInstant();

                String k = key(p.getId(), startTs);
                var existing = existingMap.get(k);

                if (existing == null) {
                    var o = new com.ttclub.backend.booking.model.ProgramOccurrence();
                    o.setProgram(p);
                    o.setSlot(s);
                    o.setCoach(s.getCoach());
                    o.setStartTs(startTs);
                    o.setEndTs(endTs);
                    o.setCancelled(false);
                    toInsert.add(o);
                    existingMap.put(k, o);
                } else {
                    boolean changed = false;

                    Long currentCoachId = existing.getCoach() != null ? existing.getCoach().getId() : null;
                    Long desiredCoachId = s.getCoach().getId();

                    if (!Objects.equals(currentCoachId, desiredCoachId)) {
                        existing.setCoach(s.getCoach());
                        changed = true;
                    }
                    if (!Objects.equals(existing.getEndTs(), endTs)) {
                        existing.setEndTs(endTs);
                        changed = true;
                    }
                    if (existing.isCancelled()) {
                        existing.setCancelled(false); // revive if schedule once again requires this start
                        changed = true;
                    }
                    if (changed) {
                        toUpdate.add(existing);
                    }
                }

                d = d.plusWeeks(1);
            }
        }

        return skippedNoCoach;
    }

    private static String key(Long programId, Instant start) {
        return (programId != null ? programId : 0L) + "|" + start.toEpochMilli();
    }

    /* Feeds */

    @Transactional
    public List<ProgramOccurrenceDto> publicFeed(Instant from, Instant to) {
        return toDtos(occurrences.findAllInRangeFetch(from, to));
    }

    @Transactional
    public List<ProgramOccurrenceDto> adminFeed(Long programId, Instant from, Instant to) {
        return toDtos(occurrences.findByProgramInRangeFetch(programId, from, to));
    }

    private static List<ProgramOccurrenceDto> toDtos(List<com.ttclub.backend.booking.model.ProgramOccurrence> list) {
        return list.stream().map(o -> {
            ProgramOccurrenceDto d = new ProgramOccurrenceDto();
            d.id        = o.getId();                                        
            d.programId = o.getProgram() != null ? o.getProgram().getId() : null;
            d.title     = (o.getProgram() != null ? o.getProgram().getTitle() : null);
            d.start     = o.getStartTs();
            d.end       = o.getEndTs();
            d.coachName = displayName(o.getCoach());
            return d;
        }).toList();
    }

    private static String displayName(User u) {
        if (u == null) return null;
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String full = (fn + " " + ln).trim();
        if (!full.isEmpty()) return full;
        return u.getEmail() != null ? u.getEmail() : ("User#" + u.getId());
    }

    /* Cancel or Rebuild */

    /**
     * Cancel (soft-hide) all future occurrences for a program from 'fromTs' onward,
     * skipping any that already have attendance.
     * @return number of occurrences cancelled
     */
    @Transactional
    public int cancelFuture(Long programId, Instant fromTs) {
        List<com.ttclub.backend.booking.model.ProgramOccurrence> list =
                occurrences.findActiveFutureForProgram(programId, fromTs);

        if (list.isEmpty()) return 0;

        var ids = list.stream().map(com.ttclub.backend.booking.model.ProgramOccurrence::getId).toList();
        var withAttendance = new HashSet<>(attendance.findAnyByOccurrenceIds(ids));

        int cancelled = 0;
        for (var o : list) {
            if (withAttendance.contains(o.getId())) continue; // protect attended
            if (!o.isCancelled()) {
                o.setCancelled(true);
                cancelled++;
            }
        }
        if (cancelled > 0) occurrences.saveAll(list);
        log.info("Cancelled {} future occurrences for program {} from {} ({} protected by attendance).",
                cancelled, programId, fromTs, withAttendance.size());
        return cancelled;
    }

    /**
     * Rebuild a program's window: cancel future (without attendance) then materialize for that window.
     * Returns the number of new rows inserted (updates are logged).
     */
    @Transactional
    public int rebuildProgramWindow(Long programId, LocalDate fromDate, LocalDate toDate) {
        ZoneId zone = ZoneId.systemDefault();
        Instant from = fromDate.atStartOfDay(zone).toInstant();
        cancelFuture(programId, from);

        Program p = programs.findById(programId).orElseThrow();
        List<ProgramSlot> programSlots = slots.findByProgramIdOrderByWeekdayAscStartTimeAsc(programId);
        if (programSlots.isEmpty()) return 0;

        var existingList = occurrences.findAllForProgramInRange(
                programId, from, toDate.plusDays(1).atStartOfDay(zone).toInstant());

        Map<String, com.ttclub.backend.booking.model.ProgramOccurrence> existingMap = existingList.stream()
                .collect(Collectors.toMap(
                        o -> key(programId, o.getStartTs()),
                        o -> o,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<com.ttclub.backend.booking.model.ProgramOccurrence> toInsert = new ArrayList<>();
        List<com.ttclub.backend.booking.model.ProgramOccurrence> toUpdate = new ArrayList<>();
        int skippedNoCoach = processProgramWindow(p, programSlots, fromDate, toDate, zone, existingMap, toInsert, toUpdate);

        if (!toInsert.isEmpty()) occurrences.saveAll(toInsert);
        if (!toUpdate.isEmpty()) occurrences.saveAll(toUpdate);
        log.info("Rebuild program {}: {} inserted, {} updated, {} skipped ({} .. {}).",
                programId, toInsert.size(), toUpdate.size(), skippedNoCoach, fromDate, toDate);
        return toInsert.size();
    }

    @Transactional
    public int materializeDefaultWindow() {
        return materializeUpcoming(DEFAULT_WEEKS);
    }
}
