package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.TableRentalDtos;
import com.ttclub.backend.booking.model.TableRentalCredit;
import com.ttclub.backend.booking.repository.TableRentalCreditRepository;
import com.ttclub.backend.booking.service.TableCreditCalculatorService;
import com.ttclub.backend.booking.service.TableCreditConsumptionService;
import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/table-credits")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminTableCreditsController {

    private final UserRepository users;
    private final TableCreditCalculatorService calc;
    private final TableCreditConsumptionService consumeSvc;
    private final TableRentalCreditRepository trcRepo;

    @PersistenceContext
    private EntityManager em;

    public AdminTableCreditsController(UserRepository users,
                                       TableCreditCalculatorService calc,
                                       TableCreditConsumptionService consumeSvc,
                                       TableRentalCreditRepository trcRepo) {
        this.users = users;
        this.calc = calc;
        this.consumeSvc = consumeSvc;
        this.trcRepo = trcRepo;
    }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
    public record ConsumeReq(Long userId, Double hours) {}
    public record ConsumeResp(boolean ok, Double hoursRemaining) {}

    @GetMapping("/users")
    public PageDto<TableRentalDtos.AdminUserCreditRow> search(@RequestParam(name = "q", required = false) String q,
                                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                                              @RequestParam(name = "size", defaultValue = "10") int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> root = cq.from(User.class);

        List<Predicate> ps = new ArrayList<>();
        ps.add(cb.equal(root.get("role").get("name"), RoleName.CLIENT));

        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase().trim() + "%";
            Predicate byEmail = cb.like(cb.lower(root.get("email")), like);
            Predicate byFirst = cb.like(cb.lower(root.get("firstName")), like);
            Predicate byLast  = cb.like(cb.lower(root.get("lastName")), like);
            ps.add(cb.or(byEmail, byFirst, byLast));
        }

        cq.where(ps.toArray(Predicate[]::new))
                .orderBy(cb.asc(root.get("firstName")), cb.asc(root.get("lastName")), cb.asc(root.get("email")));

        int pageSafe = Math.max(0, page);
        int sizeSafe = Math.min(Math.max(1, size), 100);
        var query = em.createQuery(cq)
                .setFirstResult(pageSafe * sizeSafe)
                .setMaxResults(sizeSafe);
        List<User> rows = query.getResultList();

        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<User> r2 = countQ.from(User.class);
        List<Predicate> cps = new ArrayList<>();
        cps.add(cb.equal(r2.get("role").get("name"), RoleName.CLIENT));
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase().trim() + "%";
            Predicate byEmail = cb.like(cb.lower(r2.get("email")), like);
            Predicate byFirst = cb.like(cb.lower(r2.get("firstName")), like);
            Predicate byLast  = cb.like(cb.lower(r2.get("lastName")), like);
            cps.add(cb.or(byEmail, byFirst, byLast));
        }
        countQ.select(cb.count(r2)).where(cps.toArray(Predicate[]::new));
        long total = em.createQuery(countQ).getSingleResult();

        List<TableRentalDtos.AdminUserCreditRow> content = rows.stream().map(u -> {
            var d = new TableRentalDtos.AdminUserCreditRow();
            d.id = u.getId();
            String fn = u.getFirstName() == null ? "" : u.getFirstName().trim();
            String ln = u.getLastName() == null ? "" : u.getLastName().trim();
            String name = (fn + " " + ln).trim();
            d.name = name.isEmpty() ? u.getEmail() : name;
            d.email = u.getEmail();
            // Calls the robust, lazy-safe calculator (no proxy traversal inside)
            d.tableHoursRemaining = calc.hoursAvailableForUser(u.getId()).doubleValue();
            return d;
        }).toList();

        return new PageDto<>(content, pageSafe, sizeSafe, total, (int) Math.ceil(total / (double) sizeSafe));
    }

    @PostMapping("/consume")
    @Transactional
    public ConsumeResp consume(@RequestBody ConsumeReq req,
                               @AuthenticationPrincipal User admin) {
        BigDecimal remaining = consumeSvc.consume(req.userId(), BigDecimal.valueOf(req.hours()), admin.getId());
        return new ConsumeResp(true, remaining.doubleValue());
    }

    /* Manual grant  */

    public record ManualGrantReq(Long userId, Double hours, String paymentRef, String paidAt, String notes) {}
    public record ManualGrantResp(boolean ok, Long creditId) {}

    @PostMapping("/manual-grant")
    @Transactional
    public ManualGrantResp manualGrant(@RequestBody ManualGrantReq req) {
        if (req.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (req.hours() == null || req.hours() <= 0) {
            throw new IllegalArgumentException("hours must be positive");
        }
        BigDecimal hours = BigDecimal.valueOf(req.hours()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal halfSteps = hours.multiply(new BigDecimal("2"));
        if (halfSteps.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("hours must be a multiple of 0.5");
        }

        User u = users.findById(req.userId()).orElseThrow();

        TableRentalCredit trc = new TableRentalCredit();
        trc.setUser(u);
        trc.setGroup(null);
        trc.setSourcePlan(null);
        trc.setHoursRemaining(hours);
        trcRepo.save(trc);

        return new ManualGrantResp(true, trc.getId());
    }
}
