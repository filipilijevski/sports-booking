package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.model.*;
import com.ttclub.backend.booking.repository.*;
import com.ttclub.backend.booking.service.MembershipGuard;
import com.ttclub.backend.booking.service.MembershipPaymentOrchestrator;
import com.ttclub.backend.booking.service.DuplicateEnrollmentException;
import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import com.ttclub.backend.service.TaxService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/booking/manual")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminManualBookingController {

    private final UserRepository users;
    private final MembershipPlanRepository plans;
    private final MembershipPaymentRepository membershipPayments;
    private final UserMembershipRepository userMemberships;
    private final MembershipPaymentOrchestrator membershipOrchestrator;

    private final ProgramRepository programs;
    private final ProgramPackageRepository packages;
    private final UserProgramEnrollmentRepository enrollments;
    private final ProgramEnrollmentPaymentRepository programEnrollmentPayments;

    private final TaxService tax;
    private final MembershipGuard guard;

    public AdminManualBookingController(UserRepository users,
                                        MembershipPlanRepository plans,
                                        MembershipPaymentRepository membershipPayments,
                                        UserMembershipRepository userMemberships,
                                        MembershipPaymentOrchestrator membershipOrchestrator,
                                        ProgramRepository programs,
                                        ProgramPackageRepository packages,
                                        UserProgramEnrollmentRepository enrollments,
                                        ProgramEnrollmentPaymentRepository programEnrollmentPayments,
                                        TaxService tax,
                                        MembershipGuard guard) {
        this.users = users;
        this.plans = plans;
        this.membershipPayments = membershipPayments;
        this.userMemberships = userMemberships;
        this.membershipOrchestrator = membershipOrchestrator;
        this.programs = programs;
        this.packages = packages;
        this.enrollments = enrollments;
        this.programEnrollmentPayments = programEnrollmentPayments;
        this.tax = tax;
        this.guard = guard;
    }

    /* Manual Membership */

    public record ManualMembershipReq(Long userId, Long planId, String paymentRef, Instant paidAt, String notes) {}
    public record ManualMembershipResp(boolean ok, Long paymentId) {}

    @PostMapping("/membership")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ManualMembershipResp manualMembership(@RequestBody ManualMembershipReq req) {
        Objects.requireNonNull(req.userId(), "userId required");
        Objects.requireNonNull(req.planId(), "planId required");

        User user = users.findById(req.userId()).orElseThrow();
        if (user.getRole() == null || user.getRole().getName() != RoleName.CLIENT) {
            throw new IllegalStateException("Only clients can be enrolled or assigned memberships.");
        }

        MembershipPlan plan = plans.findById(req.planId()).orElseThrow();

        // Reuse our rules
        if (plan.getType() == MembershipPlanType.INITIAL &&
                guard.hasActiveInitialMembership(user.getId())) {
            throw new IllegalStateException("You already have an active initial membership.");
        }
        if (plan.getType() == MembershipPlanType.SPECIAL &&
                !guard.hasActiveInitialMembership(user.getId())) {
            throw new IllegalStateException("Initial annual club membership is required to purchase this plan.");
        }
        if (userMemberships.existsActiveForUserAndPlan(user.getId(), plan.getId(), Instant.now())) {
            throw new IllegalStateException("An active membership for this plan already exists.");
        }

        Instant start = Optional.ofNullable(req.paidAt()).orElse(Instant.now());
        Instant end   = start.plusSeconds((long) plan.getDurationDays() * 86400);

        BigDecimal price = plan.getPriceCad();
        BigDecimal taxAmt = tax.calculate(price);
        BigDecimal total = price.add(taxAmt);

        // Create a synthetic "manual" payment row and mark as PENDING first.
        // The orchestrator will flip to SUCCEEDED and provision memberships.
        MembershipPayment mp = new MembershipPayment();
        mp.setUser(user);
        mp.setPlan(plan);
        mp.setStartTs(start);
        mp.setEndTs(end);
        mp.setPriceCad(price);
        mp.setTaxCad(taxAmt);
        mp.setTotalCad(total);
        mp.setStatus(MembershipPayment.Status.PENDING); // fix (caused no-op in orchestrator)
        String ref = (req.paymentRef() != null && !req.paymentRef().isBlank())
                ? req.paymentRef().trim()
                : ("manual:" + UUID.randomUUID());
        mp.setStripePaymentIntentId(ref);
        membershipPayments.save(mp);

        // Provision memberships + entitlements via the same orchestrator as webhooks
        membershipOrchestrator.onSucceeded(null, mp.getId());

        return new ManualMembershipResp(true, mp.getId());
    }

    /* Manual Program Enrollment */

    public record ManualEnrollmentReq(Long userId, Long programPackageId, String paymentRef, Instant paidAt, String notes) {}
    public record ManualEnrollmentResp(boolean ok, Long enrollmentId, Long paymentId) {}

    @PostMapping("/enrollment")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ManualEnrollmentResp manualEnrollment(@RequestBody ManualEnrollmentReq req) {
        Objects.requireNonNull(req.userId(), "userId required");
        Objects.requireNonNull(req.programPackageId(), "programPackageId required");

        User user = users.findById(req.userId()).orElseThrow();
        if (user.getRole() == null || user.getRole().getName() != RoleName.CLIENT) {
            throw new IllegalStateException("Only clients can be enrolled or assigned memberships.");
        }

        ProgramPackage pack = packages.findById(req.programPackageId()).orElseThrow();
        Program program = programs.findById(pack.getProgram().getId()).orElseThrow();

        // Business rules:
        guard.ensureInitialMembershipActive(user.getId()); // must have active initial
        boolean dup = enrollments.existsByUser_IdAndProgram_IdAndStatus(user.getId(), program.getId(), UserProgramEnrollment.Status.ACTIVE);
        if (dup) {
            throw new DuplicateEnrollmentException("Active enrollment already exists");
        }

        Instant start = Optional.ofNullable(req.paidAt()).orElse(Instant.now());

        // Create ACTIVE enrollment with sessions purchased/remaining
        UserProgramEnrollment e = new UserProgramEnrollment();
        e.setUser(user);
        e.setProgram(program);
        e.setProgramPackage(pack);
        e.setSessionsPurchased(pack.getSessionsCount());
        e.setSessionsRemaining(pack.getSessionsCount());
        e.setStatus(UserProgramEnrollment.Status.ACTIVE);
        e.setStartTs(start);
        e.setCreatedAt(start);
        enrollments.save(e);

        // Finance write: mirror online ProgramEnrollmentPayment rows
        Long paymentId = null;
        BigDecimal price = pack.getPriceCad();
        BigDecimal taxAmt = tax.calculate(price);
        BigDecimal total = price.add(taxAmt);
        String ref = (req.paymentRef() != null && !req.paymentRef().isBlank())
                ? req.paymentRef().trim()
                : ("manual:" + UUID.randomUUID());

        ProgramEnrollmentPayment pep = new ProgramEnrollmentPayment();
        pep.setUser(user);
        pep.setProgram(program);
        pep.setProgramPackage(pack);
        pep.setPriceCad(price);
        pep.setTaxCad(taxAmt);
        pep.setTotalCad(total);
        pep.setStatus(ProgramEnrollmentPayment.Status.SUCCEEDED);
        pep.setStripePaymentIntentId(ref);
        pep = programEnrollmentPayments.save(pep);
        paymentId = pep.getId();

        return new ManualEnrollmentResp(true, e.getId(), paymentId);
    }
}
