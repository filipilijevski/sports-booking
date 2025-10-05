package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.dto.ProgramCreateReq;
import com.ttclub.backend.booking.dto.ProgramDto;
import com.ttclub.backend.booking.dto.ProgramPackageCreateReq;
import com.ttclub.backend.booking.dto.ProgramPackageDto;
import com.ttclub.backend.booking.dto.ProgramPackageUpdateReq;
import com.ttclub.backend.booking.dto.ProgramSlotCreateReq;
import com.ttclub.backend.booking.dto.ProgramSlotDto;
import com.ttclub.backend.booking.dto.ProgramSlotUpdateReq;
import com.ttclub.backend.booking.dto.ProgramUpdateReq;
import com.ttclub.backend.booking.model.Program;
import com.ttclub.backend.booking.model.ProgramEnrollmentMode;
import com.ttclub.backend.booking.model.ProgramPackage;
import com.ttclub.backend.booking.model.ProgramSlot;
import com.ttclub.backend.booking.repository.ProgramPackageRepository;
import com.ttclub.backend.booking.repository.ProgramRepository;
import com.ttclub.backend.booking.repository.ProgramSlotRepository;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminProgramService - program/packages/slots CRUD with performance-conscious reads.<br>
 * Notes:
 *  - listPrograms() was refactored to bulk-load packages and slots to avoid N+1 queries
 *    while keeping DTOs and endpoints unchanged.
 *  - All write paths and single-item getters are preserved byte-for-byte to minimize risk.
 */
@Service
public class AdminProgramService {

    private static final Logger log = LoggerFactory.getLogger(AdminProgramService.class);

    private final ProgramRepository programs;
    private final ProgramPackageRepository packages;
    private final ProgramSlotRepository slots;
    private final UserRepository users;
    private final ProgramOccurrenceService occurrenceService;
    private final BookingMapper mapper = new BookingMapper();

    public AdminProgramService(
            ProgramRepository programs,
            ProgramPackageRepository packages,
            ProgramSlotRepository slots,
            UserRepository users,
            ProgramOccurrenceService occurrenceService
    ) {
        this.programs = programs;
        this.packages = packages;
        this.slots = slots;
        this.users = users;
        this.occurrenceService = occurrenceService;
    }

    /* Program CRUD */

    @Transactional
    public ProgramDto createProgram(ProgramCreateReq req) {
        Program p = new Program();
        p.setTitle(Objects.requireNonNull(req.title, "title"));
        p.setDescription(req.description);
        p.setActive(req.active != null ? req.active : true);
        if (req.enrollmentMode != null) {
            p.setEnrollmentMode(parseMode(req.enrollmentMode));
        }
        p = programs.save(p);
        return mapper.toProgramDto(
                p,
                List.of(),
                List.of(),
                Map.of()
        );
    }

    @Transactional
    public ProgramDto updateProgram(Long id, ProgramUpdateReq req) {
        Program p = programs.findById(id).orElseThrow();
        if (req.title != null)       p.setTitle(req.title);
        if (req.description != null) p.setDescription(req.description);
        if (req.active != null)      p.setActive(req.active);
        if (req.enrollmentMode != null) {
            p.setEnrollmentMode(parseMode(req.enrollmentMode));
        }
        programs.save(p);
        return getProgram(id);
    }

    @Transactional
    public void deleteProgram(Long id) {
        programs.deleteById(id);
    }

    @Transactional
    public ProgramDto getProgram(Long id) {
        Program p = programs.findById(id).orElseThrow();
        var packList = packages.findByProgramIdOrderBySortOrderAscIdAsc(id);
        var slotList = slots.findByProgramIdOrderByWeekdayAscStartTimeAsc(id);
        Map<Long, String> coachNames = toCoachNameMap(slotList);
        return mapper.toProgramDto(p, packList, slotList, coachNames);
    }

    /**
     * Optimized: load packages and slots for all programs in bulk to avoid N+1.
     */
    @Transactional
    public List<ProgramDto> listPrograms() {
        List<Program> all = programs.findAll();
        if (all.isEmpty()) return List.of();

        List<Long> ids = all.stream().map(Program::getId).toList();

        // Bulk fetch related entities once
        List<ProgramPackage> allPacks = packages.findByProgram_IdInOrderBySortOrderAscIdAsc(ids);
        List<ProgramSlot> allSlots = slots.findByProgram_IdInOrderByWeekdayAscStartTimeAsc(ids);

        // Build global coach-name map (one query for all coaches)
        Map<Long, String> coachNames = toCoachNameMap(allSlots);

        // Group by program id
        Map<Long, List<ProgramPackage>> packsByPid = allPacks.stream()
                .collect(Collectors.groupingBy(pp -> pp.getProgram().getId()));
        Map<Long, List<ProgramSlot>> slotsByPid = allSlots.stream()
                .collect(Collectors.groupingBy(ps -> ps.getProgram().getId()));

        // Assemble DTOs
        return all.stream()
                .map(p -> mapper.toProgramDto(
                        p,
                        packsByPid.getOrDefault(p.getId(), List.of()),
                        slotsByPid.getOrDefault(p.getId(), List.of()),
                        coachNames
                ))
                .collect(Collectors.toList());
    }

    /* Packages */

    @Transactional
    public ProgramPackageDto addPackage(Long programId, ProgramPackageCreateReq req) {
        validatePackagePayload(req);

        long active = packages.countByProgramIdAndActiveTrue(programId);
        boolean toActive = req.active == null || req.active;
        if (toActive && active >= 5) {
            throw new IllegalStateException("Cannot have more than 5 active packages for a program.");
        }

        Program p = programs.findById(programId).orElseThrow();
        ProgramPackage e = new ProgramPackage();
        e.setProgram(p);
        e.setName(req.name);
        e.setSessionsCount(req.sessionsCount);
        e.setPriceCad(toBigDecimal(req.priceCad));
        e.setActive(toActive);
        e.setSortOrder(req.sortOrder);
        validatePackageEntity(e);
        e = packages.save(e);
        return mapper.toPackageDto(e);
    }

    @Transactional
    public ProgramPackageDto updatePackage(Long id, ProgramPackageUpdateReq req) {
        ProgramPackage e = packages.findById(id).orElseThrow();
        if (req.name != null)          e.setName(req.name);
        if (req.sessionsCount != null) e.setSessionsCount(req.sessionsCount);
        if (req.priceCad != null)      e.setPriceCad(toBigDecimal(req.priceCad));
        if (req.sortOrder != null)     e.setSortOrder(req.sortOrder);
        if (req.active != null) {
            if (req.active && !e.isActive()) {
                long active = packages.countByProgramIdAndActiveTrue(e.getProgram().getId());
                if (active >= 5) {
                    throw new IllegalStateException("Cannot activate more than 5 packages.");
                }
            }
            e.setActive(req.active);
        }
        validatePackageEntity(e);
        e = packages.save(e);
        return mapper.toPackageDto(e);
    }

    @Transactional
    public void deletePackage(Long id) {
        packages.deleteById(id);
    }

    /* Slots */

    @Transactional
    public ProgramSlotDto addSlot(Long programId, ProgramSlotCreateReq req) {
        validateSlotPayload(req);
        Program p = programs.findById(programId).orElseThrow();

        ProgramSlot s = new ProgramSlot();
        s.setProgram(p);
        s.setWeekday(req.weekday);
        s.setStartTime(req.startTime);
        s.setEndTime(req.endTime);

        User coach = users.findById(req.coachId).orElseThrow();
        s.setCoach(coach);

        s = slots.save(s);

        // Immediately materialize for 12 weeks for this program
        int created = occurrenceService.materializeForProgram(programId, 12);
        log.debug("After addSlot for program {} -> materialized {} new occurrences.", programId, created);

        Map<Long, String> coachMap = new HashMap<>();
        coachMap.put(coach.getId(), displayName(coach));

        return mapper.toSlotDto(s, coachMap);
    }

    @Transactional
    public ProgramSlotDto updateSlot(Long id, ProgramSlotUpdateReq req) {
        ProgramSlot s = slots.findById(id).orElseThrow();
        if (req.weekday != null)    s.setWeekday(req.weekday);
        if (req.startTime != null)  s.setStartTime(req.startTime);
        if (req.endTime != null)    s.setEndTime(req.endTime);
        if (req.coachId != null) {
            User coach = users.findById(req.coachId).orElseThrow();
            s.setCoach(coach);
        }
        validateSlotEntity(s);
        s = slots.save(s);

        // Re-materialize future window for the owning program (safe/idempotent)
        int created = occurrenceService.materializeForProgram(s.getProgram().getId(), 12);
        log.debug("After updateSlot for program {} -> materialized {} new occurrences.",
                s.getProgram().getId(), created);

        Map<Long, String> coachMap = new HashMap<>();
        if (s.getCoach() != null) {
            coachMap.put(s.getCoach().getId(), displayName(s.getCoach()));
        }
        return mapper.toSlotDto(s, coachMap);
    }

    @Transactional
    public void deleteSlot(Long id) {
        slots.deleteById(id);
    }

    /* Helpers */

    private static BigDecimal toBigDecimal(Double v) {
        return v == null ? null : BigDecimal.valueOf(v);
    }

    private void validatePackagePayload(ProgramPackageCreateReq req) {
        if (req.sessionsCount == null || req.sessionsCount <= 0)
            throw new IllegalArgumentException("sessions_count must be > 0");
        if (req.priceCad == null || req.priceCad < 0)
            throw new IllegalArgumentException("price_cad must be >= 0");
        if (req.name == null || req.name.isBlank())
            throw new IllegalArgumentException("name is required");
    }

    private void validatePackageEntity(ProgramPackage e) {
        if (e.getSessionsCount() == null || e.getSessionsCount() <= 0)
            throw new IllegalArgumentException("sessions_count must be > 0");
        if (e.getPriceCad() == null || e.getPriceCad().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("price_cad must be >= 0");
    }

    private void validateSlotPayload(ProgramSlotCreateReq req) {
        if (req.weekday == null) throw new IllegalArgumentException("weekday required");
        if (req.startTime == null || req.endTime == null)
            throw new IllegalArgumentException("start_time and end_time required");
        if (!req.endTime.isAfter(req.startTime))
            throw new IllegalArgumentException("end_time must be after start_time");
        if (req.coachId == null) throw new IllegalArgumentException("coach_id required");
        if (users.findById(req.coachId).isEmpty())
            throw new IllegalArgumentException("coach not found");
    }

    private void validateSlotEntity(ProgramSlot e) {
        if (!e.getEndTime().isAfter(e.getStartTime()))
            throw new IllegalArgumentException("end_time must be after start_time");
    }

    /**
     * Build a coach-name map for the given slot list using a single users.findAllById call.
     */
    private Map<Long, String> toCoachNameMap(List<ProgramSlot> slotList) {
        Set<Long> ids = slotList.stream()
                .map(ProgramSlot::getCoachId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) return map;

        users.findAllById(ids).forEach(u -> map.put(u.getId(), displayName(u)));
        return map;
    }

    private static String displayName(User u) {
        if (u == null) return null;
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String full = (fn + " " + ln).trim();
        if (!full.isEmpty()) return full;
        return u.getEmail() != null ? u.getEmail() : ("User#" + u.getId());
    }

    /** Accepts OPEN/ADMIN_ONLY - also maps PUBLIC -> OPEN for back-compatibility */
    private static ProgramEnrollmentMode parseMode(String raw) {
        if (raw == null) return null;
        String n = raw.trim().toUpperCase();
        if ("PUBLIC".equals(n)) n = "OPEN";
        return ProgramEnrollmentMode.valueOf(n);
    }
}
