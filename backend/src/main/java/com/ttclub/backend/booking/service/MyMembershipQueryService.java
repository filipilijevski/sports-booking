package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.dto.MyMembershipDtos.*;
import com.ttclub.backend.booking.model.*;
import com.ttclub.backend.booking.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MyMembershipQueryService {

    private final UserMembershipRepository userMemberships;
    private final MembershipEntitlementRepository entitlements;
    private final TableRentalCreditRepository trcRepo;
    private final UserMembershipCounterRepository counters;
    private final MembershipGroupCounterRepository groupCounters;

    public MyMembershipQueryService(UserMembershipRepository userMemberships,
                                    MembershipEntitlementRepository entitlements,
                                    TableRentalCreditRepository trcRepo,
                                    UserMembershipCounterRepository counters,
                                    MembershipGroupCounterRepository groupCounters) {
        this.userMemberships = userMemberships;
        this.entitlements = entitlements;
        this.trcRepo = trcRepo;
        this.counters = counters;
        this.groupCounters = groupCounters;
    }

    @Transactional
    public MyMembershipList listForUser(Long userId) {
        List<UserMembership> list = userMemberships.findByUser_Id(userId);
        Instant now = Instant.now();

        List<MyMembership> items = list.stream().map(um -> {
            MyMembership m = new MyMembership();
            m.userMembershipId = um.getId();
            m.planId = um.getPlan().getId();
            m.planName = um.getPlan().getName();
            m.planType = um.getPlan().getType().name();
            m.holderKind = um.getPlan().getHolderKind().name();
            m.groupId = um.getGroup() != null ? um.getGroup().getId() : null;
            m.isGroupOwner = um.getGroup() != null &&
                    um.getGroup().getOwner() != null &&
                    Objects.equals(um.getGroup().getOwner().getId(), userId);
            m.startTs = um.getStartTs();
            m.endTs = um.getEndTs();
            m.active = Boolean.TRUE.equals(um.getActive())
                    && !now.isBefore(um.getStartTs())
                    && !now.isAfter(um.getEndTs());

            m.entitlements = remaining(um);
            m.daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(),
                    um.getEndTs().atZone(ZoneId.systemDefault()).toLocalDate());
            m.canRenew = !m.active || now.isAfter(um.getEndTs());
            return m;
        }).sorted(Comparator.comparing((MyMembership m) -> m.active).reversed()
                .thenComparing(m -> m.endTs)).collect(Collectors.toList());

        MyMembershipList out = new MyMembershipList();
        out.items = items;
        return out;
    }

    /* helpers */

    private Entitlements remaining(UserMembership um) {
        Entitlements e = new Entitlements();
        List<MembershipEntitlement> ents = entitlements.findByPlanId(um.getPlan().getId());

        if (um.getPlan().getHolderKind() == MembershipHolderKind.INDIVIDUAL) {
            for (MembershipEntitlement me : ents) {
                switch (me.getKind()) {
                    case TABLE_HOURS -> {
                        BigDecimal hours = trcRepo.sumHoursForIndividual(
                                um.getUser().getId(), um.getPlan().getId(), um.getStartTs(), um.getEndTs());
                        e.tableHoursRemaining = hours.doubleValue();
                    }
                    case PROGRAM_CREDITS -> {
                        BigDecimal consumed = counters
                                .findByUserMembership_IdAndKind(um.getId(), EntitlementKind.PROGRAM_CREDITS)
                                .map(c -> c.getAmountConsumed()).orElse(BigDecimal.ZERO);
                        e.programCreditsRemaining = me.getAmount().subtract(consumed).doubleValue();
                    }
                    case TOURNAMENT_ENTRIES -> {
                        BigDecimal consumed = counters
                                .findByUserMembership_IdAndKind(um.getId(), EntitlementKind.TOURNAMENT_ENTRIES)
                                .map(c -> c.getAmountConsumed()).orElse(BigDecimal.ZERO);
                        e.tournamentEntriesRemaining = me.getAmount().subtract(consumed).doubleValue();
                    }
                }
            }
        } else { // GROUP
            Long gid = um.getGroup() != null ? um.getGroup().getId() : null;
            if (gid != null) {
                for (MembershipEntitlement me : ents) {
                    switch (me.getKind()) {
                        case TABLE_HOURS -> {
                            BigDecimal hours = trcRepo.sumHoursByGroupId(gid);
                            e.tableHoursRemaining = hours.doubleValue();
                        }
                        case PROGRAM_CREDITS -> {
                            BigDecimal consumed = groupCounters
                                    .findByGroup_IdAndKind(gid, EntitlementKind.PROGRAM_CREDITS)
                                    .map(c -> c.getAmountConsumed()).orElse(BigDecimal.ZERO);
                            e.programCreditsRemaining = me.getAmount().subtract(consumed).doubleValue();
                        }
                        case TOURNAMENT_ENTRIES -> {
                            BigDecimal consumed = groupCounters
                                    .findByGroup_IdAndKind(gid, EntitlementKind.TOURNAMENT_ENTRIES)
                                    .map(c -> c.getAmountConsumed()).orElse(BigDecimal.ZERO);
                            e.tournamentEntriesRemaining = me.getAmount().subtract(consumed).doubleValue();
                        }
                    }
                }
            }
        }
        return e;
    }
}
