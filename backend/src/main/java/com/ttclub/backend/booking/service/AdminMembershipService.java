package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.dto.MembershipEntitlementDto;
import com.ttclub.backend.booking.dto.MembershipPlanDto;
import com.ttclub.backend.booking.dto.EntitlementCreateReq;
import com.ttclub.backend.booking.dto.EntitlementUpdateReq;
import com.ttclub.backend.booking.dto.PlanCreateReq;
import com.ttclub.backend.booking.dto.PlanUpdateReq;
import com.ttclub.backend.booking.model.EntitlementKind;
import com.ttclub.backend.booking.model.MembershipEntitlement;
import com.ttclub.backend.booking.model.MembershipPlan;
import com.ttclub.backend.booking.model.MembershipPlanType;
import com.ttclub.backend.booking.model.MembershipHolderKind;
import com.ttclub.backend.booking.repository.MembershipEntitlementRepository;
import com.ttclub.backend.booking.repository.MembershipPlanRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminMembershipService {

    private final MembershipPlanRepository plans;
    private final MembershipEntitlementRepository entitlements;
    private final BookingMapper mapper = new BookingMapper();

    public AdminMembershipService(MembershipPlanRepository plans,
                                  MembershipEntitlementRepository entitlements) {
        this.plans = plans;
        this.entitlements = entitlements;
    }

    /* Plan CRUD */

    @Transactional
    public MembershipPlanDto createPlan(PlanCreateReq req) {
        validatePlan(req);

        MembershipPlan p = new MembershipPlan();
        p.setType(MembershipPlanType.valueOf(req.type));
        p.setHolderKind(req.holderKind == null ? MembershipHolderKind.INDIVIDUAL
                : MembershipHolderKind.valueOf(req.holderKind)); 
        p.setName(req.name);
        p.setPriceCad(toBigDecimal(req.priceCad));
        p.setDurationDays(req.durationDays);
        p.setDescription(req.description);
        p.setActive(req.active != null ? req.active : true);
        p = plans.save(p);

        return toDto(p, List.of());
    }

    @Transactional
    public MembershipPlanDto updatePlan(Long id, PlanUpdateReq req) {
        MembershipPlan p = plans.findById(id).orElseThrow();
        if (req.type != null)         p.setType(MembershipPlanType.valueOf(req.type));
        if (req.holderKind != null)   p.setHolderKind(MembershipHolderKind.valueOf(req.holderKind)); 
        if (req.name != null)         p.setName(req.name);
        if (req.priceCad != null)     p.setPriceCad(toBigDecimal(req.priceCad));
        if (req.durationDays != null) p.setDurationDays(req.durationDays);
        if (req.description != null)  p.setDescription(req.description);
        if (req.active != null)       p.setActive(req.active);
        validatePlanEntity(p);
        plans.save(p);

        var ents = entitlements.findByPlanId(p.getId());
        return toDto(p, ents);
    }

    @Transactional
    public void deletePlan(Long id) {
        entitlements.findByPlanId(id).forEach(e -> entitlements.deleteById(e.getId()));
        plans.deleteById(id);
    }

    @Transactional
    public List<MembershipPlanDto> listPlans() {
        return plans.findAll().stream()
                .map(p -> toDto(p, entitlements.findByPlanId(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MembershipPlanDto> listPublicPlans() {
        return plans.findAll().stream()
                .filter(MembershipPlan::isActive)
                .sorted(Comparator.comparing(MembershipPlan::getName, String.CASE_INSENSITIVE_ORDER))
                .map(p -> toDto(p, entitlements.findByPlanId(p.getId())))
                .collect(Collectors.toList());
    }

    /* Entitlements */

    @Transactional
    public MembershipEntitlementDto addEntitlement(Long planId, EntitlementCreateReq req) {
        var plan = plans.findById(planId).orElseThrow();
        var e = new MembershipEntitlement();
        e.setPlan(plan);
        e.setKind(EntitlementKind.valueOf(req.kind));
        e.setAmount(Objects.requireNonNull(toBigDecimal(req.amount), "amount"));
        e = entitlements.save(e);
        return mapper.toEntDto(e);
    }

    @Transactional
    public MembershipEntitlementDto updateEntitlement(Long id, EntitlementUpdateReq req) {
        var e = entitlements.findById(id).orElseThrow();
        if (req.kind != null)   e.setKind(EntitlementKind.valueOf(req.kind));
        if (req.amount != null) e.setAmount(toBigDecimal(req.amount));
        e = entitlements.save(e);
        return mapper.toEntDto(e);
    }

    @Transactional
    public void deleteEntitlement(Long id) {
        entitlements.deleteById(id);
    }

    /* Validation */

    private void validatePlan(PlanCreateReq req) {
        if (req.type == null) throw new IllegalArgumentException("type required");
        if (req.name == null || req.name.isBlank()) throw new IllegalArgumentException("name required");
        if (req.priceCad == null || req.priceCad < 0) throw new IllegalArgumentException("price_cad >= 0");
        if (req.durationDays == null || req.durationDays <= 0)
            throw new IllegalArgumentException("duration_days must be a positive integer.");

        String holder = (req.holderKind == null ? "INDIVIDUAL" : req.holderKind);
        if (MembershipPlanType.valueOf(req.type) == MembershipPlanType.INITIAL &&
                !holder.equals("INDIVIDUAL")) {
            throw new IllegalArgumentException("INITIAL membership must be INDIVIDUAL.");
        }
    }

    private void validatePlanEntity(MembershipPlan p) {
        if (p.getType() == null) throw new IllegalArgumentException("type required");
        if (p.getHolderKind() == null) p.setHolderKind(MembershipHolderKind.INDIVIDUAL);
        if (p.getType() == MembershipPlanType.INITIAL && p.getHolderKind() != MembershipHolderKind.INDIVIDUAL)
            throw new IllegalArgumentException("INITIAL membership must be INDIVIDUAL.");
        if (p.getName() == null || p.getName().isBlank()) throw new IllegalArgumentException("name required");
        if (p.getPriceCad() == null || p.getPriceCad().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("price_cad >= 0");
        if (p.getDurationDays() == null || p.getDurationDays() <= 0)
            throw new IllegalArgumentException("duration_days must be a positive integer.");
    }

    private static BigDecimal toBigDecimal(Number v) {
        if (v == null) return null;
        if (v instanceof BigDecimal b) return b;
        return BigDecimal.valueOf(v.doubleValue());
    }

    private MembershipPlanDto toDto(MembershipPlan p, List<MembershipEntitlement> ents) {
        var dto = new MembershipPlanDto();
        dto.id = p.getId();
        dto.type = p.getType() != null ? p.getType().name() : null;
        dto.holderKind = p.getHolderKind() != null ? p.getHolderKind().name() : "INDIVIDUAL"; 
        dto.name = p.getName();
        dto.priceCad = p.getPriceCad() != null ? p.getPriceCad().doubleValue() : null;
        dto.description = p.getDescription();
        dto.durationDays = p.getDurationDays();
        dto.active = p.isActive();
        dto.entitlements = ents.stream().map(mapper::toEntDto).collect(Collectors.toList());
        return dto;
    }
}
