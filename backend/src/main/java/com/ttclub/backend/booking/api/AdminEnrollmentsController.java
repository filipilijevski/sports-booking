package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.model.ProgramPackage;
import com.ttclub.backend.booking.model.UserProgramEnrollment;
import com.ttclub.backend.booking.model.UserProgramEnrollment.Status;
import com.ttclub.backend.booking.repository.UserProgramEnrollmentRepository;
import com.ttclub.backend.model.User;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/enrollments")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminEnrollmentsController {

    private final UserProgramEnrollmentRepository enrollments;

    public AdminEnrollmentsController(UserProgramEnrollmentRepository enrollments) {
        this.enrollments = enrollments;
    }

    public record Row(
            Long id,
            Long userId,
            String userName,
            String userEmail,
            Long programId,
            String programTitle,
            Long packageId,
            String packageName,
            String status,
            Integer sessionsPurchased,
            Integer sessionsRemaining,
            Instant startTs,
            Instant endTs,
            Instant lastAttendedAt
    ) {}

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    @GetMapping
    public PageDto<Row> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int pageSafe = Math.max(0, page);
        int sizeSafe = Math.min(Math.max(1, size), 100);

        Pageable pageable = PageRequest.of(pageSafe, sizeSafe, Sort.by(Sort.Direction.DESC, "id"));

        Page<UserProgramEnrollment> p;
        String qNorm = q == null ? "" : q.trim().toLowerCase();

        if (qNorm.isBlank()) {
            p = enrollments.findAllWithGraph(pageable);
        } else {
            Status status = parseStatus(qNorm);
            if (status != null) {
                p = enrollments.findByStatusWithGraph(status, pageable);
            } else {
                p = enrollments.searchByUserProgramPackage(qNorm, pageable);
            }
        }

        List<Row> rows = p.getContent().stream().map(AdminEnrollmentsController::toRow).toList();
        return new PageDto<>(rows, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private static Row toRow(UserProgramEnrollment e) {
        User u = e.getUser();
        String name = buildDisplayName(u.getFirstName(), u.getLastName());
        ProgramPackage pp = e.getProgramPackage();
        return new Row(
                e.getId(),
                u.getId(),
                name.isBlank() ? null : name,
                u.getEmail(),
                e.getProgram().getId(),
                e.getProgram().getTitle(),
                pp.getId(),
                pp.getName(),
                e.getStatus().name(),
                e.getSessionsPurchased(),
                e.getSessionsRemaining(),
                e.getStartTs(),
                e.getEndTs(),
                e.getLastAttendedAt()
        );
    }

    private static Status parseStatus(String raw) {
        String r = raw.trim().toUpperCase();
        return switch (r) {
            case "ACTIVE" -> Status.ACTIVE;
            case "EXHAUSTED" -> Status.EXHAUSTED;
            case "CANCELLED", "CANCELED" -> Status.CANCELLED;
            default -> null;
        };
    }

    private static String buildDisplayName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last  == null ? "" : last.trim();
        return (f + " " + l).trim();
    }
}
