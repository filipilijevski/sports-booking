package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.dto.AdminGroupDtos.*;
import com.ttclub.backend.booking.model.*;
import com.ttclub.backend.booking.repository.*;
import com.ttclub.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminGroupQueryService {

    private final MembershipGroupRepository groups;
    private final MembershipEntitlementRepository entitlements;
    private final UserMembershipRepository userMemberships;
    private final TableRentalCreditRepository trcRepo;
    private final MembershipGroupCounterRepository groupCounters;
    private final UserRepository users;

    public AdminGroupQueryService(MembershipGroupRepository groups,
                                  MembershipEntitlementRepository entitlements,
                                  UserMembershipRepository userMemberships,
                                  TableRentalCreditRepository trcRepo,
                                  MembershipGroupCounterRepository groupCounters,
                                  UserRepository users) {
        this.groups = groups;
        this.entitlements = entitlements;
        this.userMemberships = userMemberships;
        this.trcRepo = trcRepo;
        this.groupCounters = groupCounters;
        this.users = users;
    }

    @Transactional
    public List<GroupListItem> list() {
        List<MembershipGroup> list = groups.findAll();

        return list.stream().map(g -> {
            GroupListItem d = base(g);
            d.membersCount = userMemberships.countByGroup_Id(g.getId());

            // remaining entitlements
            d.entitlements = remainingForGroup(g);
            return d;
        }).collect(Collectors.toList());
    }

    @Transactional
    public GroupDetail get(Long groupId) {
        MembershipGroup g = groups.findById(groupId).orElseThrow();
        GroupDetail d = new GroupDetail();
        GroupListItem base = base(g);
        d.id = base.id; d.planId = base.planId; d.planName = base.planName;
        d.planType = base.planType; d.holderKind = base.holderKind;
        d.ownerId = base.ownerId; d.ownerName = base.ownerName; d.ownerEmail = base.ownerEmail;
        d.startTs = base.startTs; d.endTs = base.endTs; d.active = base.active;

        // members
        d.members = userMemberships.findByGroup_Id(groupId).stream().map(um -> {
            MemberRow mr = new MemberRow();
            mr.userMembershipId = um.getId();
            mr.userId = um.getUser().getId();
            String fn = Optional.ofNullable(um.getUser().getFirstName()).orElse("").trim();
            String ln = Optional.ofNullable(um.getUser().getLastName()).orElse("").trim();
            String name = (fn + " " + ln).trim();
            mr.name = name.isEmpty() ? um.getUser().getEmail() : name;
            mr.email = um.getUser().getEmail();
            mr.active = Boolean.TRUE.equals(um.getActive());
            return mr;
        }).collect(Collectors.toList());

        d.membersCount = d.members.size();
        d.entitlements = remainingForGroup(g);
        return d;
    }

    /* helpers  */

    private GroupListItem base(MembershipGroup g) {
        GroupListItem d = new GroupListItem();
        d.id = g.getId();
        d.planId = g.getPlan().getId();
        d.planName = g.getPlan().getName();
        d.planType = g.getPlan().getType().name();
        d.holderKind = g.getPlan().getHolderKind().name();
        d.ownerId = g.getOwner().getId();

        String fn = Optional.ofNullable(g.getOwner().getFirstName()).orElse("").trim();
        String ln = Optional.ofNullable(g.getOwner().getLastName()).orElse("").trim();
        String name = (fn + " " + ln).trim();
        d.ownerName = name.isEmpty() ? g.getOwner().getEmail() : name;

        d.ownerEmail = g.getOwner().getEmail();
        d.startTs = g.getStartTs();
        d.endTs = g.getEndTs();
        d.active = Boolean.TRUE.equals(g.getActive());
        return d;
    }

    private EntitlementsSummary remainingForGroup(MembershipGroup g) {
        EntitlementsSummary s = new EntitlementsSummary();
        List<MembershipEntitlement> ents = entitlements.findByPlanId(g.getPlan().getId());

        BigDecimal hoursRem = null;
        BigDecimal progRem = null;
        BigDecimal tourRem = null;

        // pooled table hours
        Optional<MembershipEntitlement> tableEnt = ents.stream()
                .filter(e -> e.getKind() == EntitlementKind.TABLE_HOURS).findFirst();
        if (tableEnt.isPresent()) {
            hoursRem = trcRepo.sumHoursByGroupId(g.getId());
        }
        // program credits
        Optional<MembershipEntitlement> pcEnt = ents.stream()
                .filter(e -> e.getKind() == EntitlementKind.PROGRAM_CREDITS).findFirst();
        if (pcEnt.isPresent()) {
            BigDecimal consumed = groupCounters.findByGroup_IdAndKind(g.getId(), EntitlementKind.PROGRAM_CREDITS)
                    .map(c -> c.getAmountConsumed()).orElse(BigDecimal.ZERO);
            progRem = pcEnt.get().getAmount().subtract(consumed);
        }
        // tournament entries
        Optional<MembershipEntitlement> tEnt = ents.stream()
                .filter(e -> e.getKind() == EntitlementKind.TOURNAMENT_ENTRIES).findFirst();
        if (tEnt.isPresent()) {
            BigDecimal consumed = groupCounters.findByGroup_IdAndKind(g.getId(), EntitlementKind.TOURNAMENT_ENTRIES)
                    .map(c -> c.getAmountConsumed()).orElse(BigDecimal.ZERO);
            tourRem = tEnt.get().getAmount().subtract(consumed);
        }

        s.tableHoursRemaining = hoursRem != null ? hoursRem.doubleValue() : null;
        s.programCreditsRemaining = progRem != null ? progRem.doubleValue() : null;
        s.tournamentEntriesRemaining = tourRem != null ? tourRem.doubleValue() : null;
        return s;
    }
}
