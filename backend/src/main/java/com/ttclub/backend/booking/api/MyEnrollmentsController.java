package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.model.UserProgramEnrollment;
import com.ttclub.backend.booking.repository.UserProgramEnrollmentRepository;
import com.ttclub.backend.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Returns the authenticated user's program enrollments.
 * Small and read-only.
 */
@RestController
@RequestMapping("/api/my/enrollments")
public class MyEnrollmentsController {

    private final UserProgramEnrollmentRepository enrollments;

    public MyEnrollmentsController(UserProgramEnrollmentRepository enrollments) {
        this.enrollments = enrollments;
    }

    /** GET /api/my/enrollments -> array of enrollments for the current user (most recent first). */
    @GetMapping
    public List<MyEnrollmentDto> list(Authentication auth) {
        Long userId = resolveUserId(auth);
        return enrollments.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(MyEnrollmentDto::from)
                .toList();
    }

    /**
     * GET /api/my/enrollments/active?programId=123 -> { "active": true|false }
     * Used by the UI to gate purchases.
     */
    @GetMapping("/active")
    public Map<String, Boolean> hasActive(Authentication auth,
                                          @RequestParam("programId") Long programId) {
        Long userId = resolveUserId(auth);
        boolean active = enrollments.existsByUser_IdAndProgram_IdAndStatus(
                userId, programId, UserProgramEnrollment.Status.ACTIVE);
        return Map.of("active", active);
    }

    /* helpers */

    private static Long resolveUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("Authentication required.");
        }
        Object p = auth.getPrincipal();
        if (p instanceof User u && u.getId() != null) {
            return u.getId();
        }
        // Defensive fallback - should not happen with our JwtAuthFilter, but keep it safe.
        throw new SecurityException("Invalid authentication principal.");
    }

    /*  DTO */

    /** Shape aligned with frontend MyMembershipsPayload.enrollments[]. */
    public static class MyEnrollmentDto {
        public Long   enrollmentId;
        public Long   programId;
        public String programTitle;
        public String status;                // "ACTIVE" | "EXHAUSTED" | "CANCELLED"
        public Integer sessionsPurchased;
        public Integer sessionsRemaining;
        public String  lastAttendedAt;       // ISO
        public String  startTs;              // ISO
        public String  endTs;                // ISO
        public String  packageName;

        public static MyEnrollmentDto from(UserProgramEnrollment e) {
            MyEnrollmentDto d = new MyEnrollmentDto();
            d.enrollmentId      = e.getId();
            d.programId         = e.getProgram() != null ? e.getProgram().getId() : null;
            d.programTitle      = e.getProgram() != null ? e.getProgram().getTitle() : null;
            d.status            = e.getStatus() != null ? e.getStatus().name() : null;
            d.sessionsPurchased = e.getSessionsPurchased();
            d.sessionsRemaining = e.getSessionsRemaining();
            d.lastAttendedAt    = e.getLastAttendedAt() != null ? e.getLastAttendedAt().toString() : null;
            d.startTs           = e.getStartTs() != null ? e.getStartTs().toString() : null;
            d.endTs             = e.getEndTs() != null ? e.getEndTs().toString() : null;
            d.packageName       = (e.getProgramPackage() != null ? e.getProgramPackage().getName() : null);
            return d;
        }
    }
}
