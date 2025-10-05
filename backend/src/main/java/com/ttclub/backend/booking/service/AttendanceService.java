package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.api.AttendanceController.EligibleUser;
import com.ttclub.backend.booking.api.AttendanceController.ListResp;
import com.ttclub.backend.booking.api.AttendanceController.OccurrenceDto;
import com.ttclub.backend.booking.model.Attendance;
import com.ttclub.backend.booking.model.Program;
import com.ttclub.backend.booking.model.ProgramOccurrence;
import com.ttclub.backend.booking.model.UserProgramEnrollment;
import com.ttclub.backend.booking.model.UserProgramEnrollment.Status;
import com.ttclub.backend.booking.repository.AttendanceRepository;
import com.ttclub.backend.booking.repository.ProgramOccurrenceRepository;
import com.ttclub.backend.booking.repository.UserProgramEnrollmentRepository;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final UserProgramEnrollmentRepository enrollments;
    private final ProgramOccurrenceRepository occurrences;
    private final MembershipGuard guard;
    private final UserRepository users;

    public AttendanceService(AttendanceRepository attendanceRepo,
                             UserProgramEnrollmentRepository enrollments,
                             ProgramOccurrenceRepository occurrences,
                             MembershipGuard guard,
                             UserRepository users) {
        this.attendanceRepo = attendanceRepo;
        this.enrollments = enrollments;
        this.occurrences = occurrences;
        this.guard = guard;
        this.users = users;
    }

    /* READ: eligible list */

    @Transactional
    public ListResp listEligible(Long occurrenceId, Long programId, LocalDate date) {
        ProgramOccurrence occ = resolveOccurrence(occurrenceId, programId, date);
        // program on an occurrence is mandatory by model - enforce & silence static analyzer
        Program program = Objects.requireNonNull(occ.getProgram(), "Occurrence has no associated program");

        // Load eligible ACTIVE enrollments with sessionsRemaining > 0
        List<UserProgramEnrollment> es = enrollments.findEligibleForProgram(program.getId(), Status.ACTIVE);

        // Which users are already marked PRESENT for this occurrence?
        Set<Long> presentUserIds = new HashSet<>(attendanceRepo.findUserIdsByOccurrence(occ.getId()));

        List<EligibleUser> users = es.stream().map(e -> {
            User u = e.getUser();
            String name = displayName(u);
            boolean present = presentUserIds.contains(u.getId());
            return new EligibleUser(
                    u.getId(),
                    name.isBlank() ? null : name,
                    u.getEmail(),
                    e.getId(),
                    e.getSessionsRemaining(),
                    present
            );
        }).toList();

        OccurrenceDto o = new OccurrenceDto(
                occ.getId(),
                program.getId(),
                program.getTitle(),
                occ.getStartTs().toString(),
                occ.getEndTs().toString()
        );
        return new ListResp(o, users);
    }

    private ProgramOccurrence resolveOccurrence(Long occurrenceId, Long programId, LocalDate date) {
        if (occurrenceId != null) {
            return occurrences.findById(occurrenceId).orElseThrow();
        }
        if (programId == null || date == null) {
            throw new IllegalArgumentException("Either occurrenceId or (programId + date) must be provided.");
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to   = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<ProgramOccurrence> list = occurrences.findByProgramInRangeFetch(programId, from, to);
        if (list.isEmpty()) throw new IllegalStateException("No occurrence found for the given program and date.");
        return list.get(0);
    }

    private static String displayName(User u) {
        if (u == null) return null;
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String full = (fn + " " + ln).trim();
        if (!full.isEmpty()) return full;
        return u.getEmail() != null ? u.getEmail() : ("User#" + u.getId());
    }

    /* WRITE: mark / undo */

    /**
     * Mark or undo attendance for a user in a specific occurrence.
     * - present=true - decrement sessions_remaining (>=1), write attendance row<br>
     * - present=false - delete attendance row, increment sessions back<br>
     * Enforces active INITIAL membership via MembershipGuard.
     * Optimistic lock on UserProgramEnrollment via @Version.
     */
    @Transactional
    public void markAttendance(Long occurrenceId, Long userId, boolean present, Authentication auth) {
        Objects.requireNonNull(occurrenceId, "occurrenceId is required");
        Objects.requireNonNull(userId, "userId is required");

        Long markerId = resolveMarkerId(auth);
        guard.ensureInitialMembershipActive(userId);

        ProgramOccurrence occ = occurrences.findById(occurrenceId).orElseThrow();
        Long programId = occ.getProgram().getId();

        // Find ACTIVE enrollment for this program
        UserProgramEnrollment enr = enrollments
                .findFirstByUser_IdAndProgram_IdAndStatus(userId, programId, Status.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No ACTIVE enrollment found for this program."));

        if (present) {
            // idempotent: if already present, return
            if (attendanceRepo.existsByOccurrenceIdAndUserId(occurrenceId, userId)) return;

            int rem = Optional.ofNullable(enr.getSessionsRemaining()).orElse(0);
            if (rem < 1) {
                throw new IllegalStateException("No sessions remaining in enrollment.");
            }
            enr.setSessionsRemaining(rem - 1);
            enr.setLastAttendedAt(Instant.now());

            try {
                enrollments.saveAndFlush(enr);
            } catch (ObjectOptimisticLockingFailureException ex) {
                throw new IllegalStateException("Concurrent update detected. Please retry.");
            }

            // create attendance row
            Attendance a = new Attendance();
            a.setOccurrence(occ);
            a.setUser(users.findById(userId).orElseThrow());
            a.setEnrollment(null); // optional linkage if you later introduce read-model Enrollment entity
            a.setMarkedBy(users.findById(markerId).orElseThrow());
            a.setMarkedAt(Instant.now());
            attendanceRepo.save(a);

            // auto-exhaust if zero
            if (enr.getSessionsRemaining() != null && enr.getSessionsRemaining() == 0) {
                enr.setStatus(Status.EXHAUSTED);
                enrollments.save(enr);
            }
        } else {
            // undo if present
            if (!attendanceRepo.existsByOccurrenceIdAndUserId(occurrenceId, userId)) return;

            // Remove the attendance row (safe if multiple admins)
            List<Long> uids = attendanceRepo.findUserIdsByOccurrence(occurrenceId);
            if (uids.contains(userId)) {
                // delete by simple findAll + filter (keeps repository minimal)
                attendanceRepo.findAll().stream()
                        .filter(a -> Objects.equals(a.getOccurrence().getId(), occurrenceId)
                                && Objects.equals(a.getUser().getId(), userId))
                        .forEach(attendanceRepo::delete);
            }

            // increment back
            Integer rem = Optional.ofNullable(enr.getSessionsRemaining()).orElse(0);
            enr.setSessionsRemaining(rem + 1);

            if (enr.getStatus() == Status.EXHAUSTED) {
                enr.setStatus(Status.ACTIVE);
            }
            enrollments.save(enr);
        }
    }

    private Long resolveMarkerId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("Authentication required.");
        }
        Object p = auth.getPrincipal();
        if (p instanceof User u && u.getId() != null) {
            return u.getId();
        }
        throw new SecurityException("Invalid authentication principal.");
    }
}
