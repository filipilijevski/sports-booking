package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.model.UserMembership;
import com.ttclub.backend.booking.model.UserProgramEnrollment;
import com.ttclub.backend.booking.repository.UserMembershipRepository;
import com.ttclub.backend.booking.repository.UserProgramEnrollmentRepository;
import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/booking/users") // dedicated search endpoint for enroll UI
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminUserSearchController {

    private final UserRepository users;
    private final UserMembershipRepository userMemberships;
    private final UserProgramEnrollmentRepository enrollments;

    public AdminUserSearchController(UserRepository users,
                                     UserMembershipRepository userMemberships,
                                     UserProgramEnrollmentRepository enrollments) {
        this.users = users;
        this.userMemberships = userMemberships;
        this.enrollments = enrollments;
    }

    public record MembershipInfo(Long userMembershipId, Long planId, String planName, Instant endTs) {}
    public record EnrollmentInfo(Long enrollmentId, String programTitle, String packageName, Integer sessionsRemaining) {}

    public record Row(Long id, String name, String email,
                      boolean hasActiveInitial, int activeMemberships, int activeEnrollments,
                      Integer lessonsRemaining,
                      List<MembershipInfo> memberships,
                      List<EnrollmentInfo> enrollmentsList) {}

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    /**
     * Returns CLIENTS only. Includes lists of active memberships and active enrollments
     * (with sessions remaining), plus a total lessonsRemaining (sum of sessionsRemaining).
     */
    @GetMapping
    @Transactional
    public PageDto<Row> search(@RequestParam(name = "q", required = false) String q,
                               @RequestParam(name = "page", defaultValue = "0") int page,
                               @RequestParam(name = "size", defaultValue = "10") int size) {

        int pageSafe = Math.max(0, page);
        int sizeSafe = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(pageSafe, sizeSafe, Sort.by(Sort.Direction.ASC, "id"));

        String qNorm = q == null ? "" : q.trim().toLowerCase();
        Page<User> p = qNorm.isBlank()
                ? users.findByRole_Name(RoleName.CLIENT, pageable)
                : users.searchByRoleAndNameOrEmail(RoleName.CLIENT, qNorm, pageable);

        List<Row> rows = p.getContent().stream().map(u -> {
            String name = buildDisplayName(u.getFirstName(), u.getLastName());

            boolean hasInitial = !userMemberships.findActiveInitialMembershipsForUser(u.getId()).isEmpty();

            // Active memberships list (time-windowed & active)
            List<UserMembership> all = userMemberships.findByUser_Id(u.getId());
            Instant now = Instant.now();
            List<UserMembership> activeMs = all.stream()
                    .filter(um -> Boolean.TRUE.equals(um.getActive())
                            && um.getStartTs() != null && um.getEndTs() != null
                            && !um.getStartTs().isAfter(now) && !um.getEndTs().isBefore(now))
                    .toList();

            List<MembershipInfo> memList = activeMs.stream().map(um ->
                    new MembershipInfo(um.getId(),
                            um.getPlan() != null ? um.getPlan().getId() : null,
                            um.getPlan() != null ? um.getPlan().getName() : null,
                            um.getEndTs())
            ).toList();

            int memberships = activeMs.size();

            // Active enrollments list (with sessions left)
            List<UserProgramEnrollment> ens = enrollments.findByUser_IdOrderByCreatedAtDesc(u.getId())
                    .stream()
                    .filter(e -> e.getStatus() == UserProgramEnrollment.Status.ACTIVE)
                    .toList();

            List<EnrollmentInfo> enrList = ens.stream().map(e ->
                    new EnrollmentInfo(
                            e.getId(),
                            e.getProgram() != null ? e.getProgram().getTitle() : null,
                            (e.getProgramPackage() != null ? e.getProgramPackage().getName() : null),
                            e.getSessionsRemaining()
                    )
            ).toList();

            int activeEnrs = ens.size();
            int lessonsRemaining = ens.stream()
                    .map(UserProgramEnrollment::getSessionsRemaining)
                    .filter(v -> v != null && v >= 0)
                    .mapToInt(Integer::intValue)
                    .sum();

            return new Row(
                    u.getId(),
                    name.isBlank() ? null : name,
                    u.getEmail(),
                    hasInitial,
                    memberships,
                    activeEnrs,
                    lessonsRemaining,
                    memList,
                    enrList
            );
        }).toList();

        return new PageDto<>(rows, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private static String buildDisplayName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last  == null ? "" : last.trim();
        return (f + " " + l).trim();
    }
}
