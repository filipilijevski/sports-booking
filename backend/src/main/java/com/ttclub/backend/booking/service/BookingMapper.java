package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.dto.*;
import com.ttclub.backend.booking.model.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps booking-related entities to DTOs.
 */
public class BookingMapper {

    /* Program */

    public ProgramDto toProgramDto(Program p,
                                   List<ProgramPackage> packs,
                                   List<ProgramSlot> slots,
                                   Map<Long, String> coachNames) {
        ProgramDto dto = new ProgramDto();
        dto.id = p.getId();
        dto.title = p.getTitle();
        dto.description = p.getDescription();
        dto.active = p.isActive();
        dto.enrollmentMode = (p.getEnrollmentMode() != null ? p.getEnrollmentMode().name() : "OPEN");
        dto.packages = packs.stream().map(this::toPackageDto).collect(Collectors.toList());
        dto.slots = slots.stream().map(s -> toSlotDto(s, coachNames)).collect(Collectors.toList());
        return dto;
    }

    public ProgramPackageDto toPackageDto(ProgramPackage e) {
        ProgramPackageDto d = new ProgramPackageDto();
        d.id = e.getId();
        d.programId = e.getProgram().getId();
        d.name = e.getName();
        d.sessionsCount = e.getSessionsCount();
        d.priceCad = toDouble(e.getPriceCad());
        d.active = e.isActive();
        d.sortOrder = e.getSortOrder();
        return d;
    }

    public ProgramSlotDto toSlotDto(ProgramSlot e, Map<Long,String> coachNames) {
        ProgramSlotDto d = new ProgramSlotDto();
        d.id = e.getId();
        d.programId = e.getProgram().getId();
        d.weekday = e.getWeekday();
        d.startTime = e.getStartTime();
        d.endTime = e.getEndTime();
        d.coachId = e.getCoachId();
        d.coachName = coachNames != null ? coachNames.getOrDefault(e.getCoachId(), null) : null;
        return d;
    }

    /* Public Card */

    public ProgramCardDto toCard(Program p,
                                 List<ProgramPackage> packs,
                                 List<ProgramSlot> slots,
                                 Map<Long,String> coachNames) {
        ProgramCardDto c = new ProgramCardDto();
        c.id = p.getId();
        c.title = p.getTitle();
        c.description = p.getDescription();
        c.active = p.isActive();
        c.enrollmentMode = (p.getEnrollmentMode() != null ? p.getEnrollmentMode().name() : "OPEN");

        c.packages = packs.stream().map(this::toPackageDto).collect(Collectors.toList());

        Map<Integer, List<ProgramSlot>> byDay = slots.stream()
                .collect(Collectors.groupingBy(s -> s.getWeekday().getValue(), TreeMap::new, Collectors.toList()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        List<ProgramWeeklyDto> weekly = new ArrayList<>();

        byDay.forEach((dow, list) -> {
            ProgramWeeklyDto w = new ProgramWeeklyDto();
            w.weekday = list.get(0).getWeekday();
            w.times = list.stream().map(s -> {
                TimeRange tr = new TimeRange();
                tr.start = fmt.format(s.getStartTime());
                tr.end = fmt.format(s.getEndTime());
                tr.coach = coachNames != null ? coachNames.getOrDefault(s.getCoachId(), null) : null;
                return tr;
            }).collect(Collectors.toList());
            weekly.add(w);
        });

        c.weekly = weekly;
        c.coaches = slots.stream()
                .map(s -> coachNames != null ? coachNames.getOrDefault(s.getCoachId(), null) : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return c;
    }

    /* Membership Plan */

    public MembershipPlanDto toPlanDto(MembershipPlan p, List<MembershipEntitlement> ents) {
        MembershipPlanDto d = new MembershipPlanDto();
        d.id = p.getId();
        d.type = p.getType().name();
        d.name = p.getName();
        d.priceCad = toDouble(p.getPriceCad());
        d.durationDays = p.getDurationDays();
        d.active = p.isActive();
        d.entitlements = ents.stream().map(this::toEntDto).collect(Collectors.toList());
        return d;
    }

    public MembershipEntitlementDto toEntDto(MembershipEntitlement e) {
        MembershipEntitlementDto d = new MembershipEntitlementDto();
        d.id = e.getId();
        d.planId = e.getPlan().getId();
        d.kind = e.getKind().name();
        d.amount = toDouble(e.getAmount());
        return d;
    }

    private static Double toDouble(BigDecimal v) {
        return (v == null) ? null : v.doubleValue();
    }
}
