package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.repository.TableRentalCreditRepository;
import com.ttclub.backend.booking.repository.UserMembershipRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TableCreditCalculatorService {

    private final TableRentalCreditRepository trcRepo;
    private final UserMembershipRepository userMemberships;

    public TableCreditCalculatorService(TableRentalCreditRepository trcRepo,
                                        UserMembershipRepository userMemberships) {
        this.trcRepo = trcRepo;
        this.userMemberships = userMemberships;
    }

    /**
     * Total = sum of individual TRC + pooled TRC across the user's currently-active groups.
     * This implementation performs all gating in repository queries (no lazy proxy traversal).
     * Signature is unchanged for backward compatibility with all existing call sites.
     */
    public BigDecimal hoursAvailableForUser(Long userId) {
        BigDecimal indiv = nz(trcRepo.sumHoursByUserId(userId));

        BigDecimal pooled = BigDecimal.ZERO;
        List<Long> groupIds = userMemberships.findActiveGroupIdsForUser(userId);
        if (!groupIds.isEmpty()) {
            for (Long gid : groupIds) {
                pooled = pooled.add(nz(trcRepo.sumHoursByGroupId(gid)));
            }
        }
        return indiv.add(pooled);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
