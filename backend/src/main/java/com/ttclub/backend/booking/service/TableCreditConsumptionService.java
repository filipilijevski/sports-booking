package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.model.TableRentalConsumption;
import com.ttclub.backend.booking.model.TableRentalCredit;
import com.ttclub.backend.booking.repository.TableRentalConsumptionRepository;
import com.ttclub.backend.booking.repository.TableRentalCreditRepository;
import com.ttclub.backend.booking.repository.UserMembershipRepository;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TableCreditConsumptionService {

    private final TableRentalCreditRepository trcRepo;
    private final TableRentalConsumptionRepository auditRepo;
    private final UserMembershipRepository userMemberships;
    private final TableCreditCalculatorService calculator;
    private final UserRepository users;

    public TableCreditConsumptionService(TableRentalCreditRepository trcRepo,
                                         TableRentalConsumptionRepository auditRepo,
                                         UserMembershipRepository userMemberships,
                                         TableCreditCalculatorService calculator,
                                         UserRepository users) {
        this.trcRepo = trcRepo;
        this.auditRepo = auditRepo;
        this.userMemberships = userMemberships;
        this.calculator = calculator;
        this.users = users;
    }

    @Transactional
    public BigDecimal consume(Long targetUserId, BigDecimal hours, Long adminUserId) {
        Objects.requireNonNull(targetUserId, "userId required");
        Objects.requireNonNull(hours, "hours required");
        Objects.requireNonNull(adminUserId, "adminUserId required");

        if (hours.signum() <= 0) throw new IllegalArgumentException("hours must be positive");
        BigDecimal scaled = hours.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal units = scaled.multiply(new BigDecimal("2")); // 0.5h steps
        if (units.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("hours must be a multiple of 0.5");
        }
        if (scaled.compareTo(new BigDecimal("2.5")) > 0) {
            throw new IllegalArgumentException("hours cannot exceed 2.5 per operation");
        }

        BigDecimal available = calculator.hoursAvailableForUser(targetUserId);
        if (available.compareTo(scaled) < 0) {
            throw new IllegalStateException("No sufficient table rental credits remaining.");
        }

        // resolve active group IDs via repository JPQL (no proxy traversal)
        List<Long> activeGroupIds = userMemberships.findActiveGroupIdsForUser(targetUserId);

        // lock candidate TRC rows FIFO
        List<TableRentalCredit> buckets = trcRepo.lockEligibleForUser(targetUserId, activeGroupIds);

        BigDecimal remaining = scaled;
        User client = users.findById(targetUserId).orElseThrow();
        User admin  = users.findById(adminUserId).orElseThrow();

        for (TableRentalCredit trc : buckets) {
            if (remaining.signum() <= 0) break;
            BigDecimal avail = trc.getHoursRemaining() == null ? BigDecimal.ZERO : trc.getHoursRemaining();
            if (avail.signum() <= 0) continue;

            BigDecimal take = avail.min(remaining);
            BigDecimal newBal = avail.subtract(take);
            trc.setHoursRemaining(newBal);
            trcRepo.save(trc);

            TableRentalConsumption c = new TableRentalConsumption();
            c.setUser(client);
            c.setConsumedBy(admin);
            c.setTrc(trc);
            c.setGroup(trc.getGroup());
            c.setAmountHours(take);
            auditRepo.save(c);

            remaining = remaining.subtract(take);
        }

        if (remaining.signum() > 0) {
            // Defensive guard against race conditions
            throw new IllegalStateException("Concurrent consumption detected. Please retry.");
        }

        return calculator.hoursAvailableForUser(targetUserId);
    }
}
