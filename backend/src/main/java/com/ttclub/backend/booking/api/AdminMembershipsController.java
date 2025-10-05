package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.MembershipEntitlementDto;
import com.ttclub.backend.booking.dto.MembershipPlanDto;
import com.ttclub.backend.booking.dto.EntitlementCreateReq;
import com.ttclub.backend.booking.dto.EntitlementUpdateReq;
import com.ttclub.backend.booking.dto.PlanCreateReq;
import com.ttclub.backend.booking.dto.PlanUpdateReq;
import com.ttclub.backend.booking.service.AdminMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/memberships")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminMembershipsController {

    private final AdminMembershipService svc;

    public AdminMembershipsController(AdminMembershipService svc) { this.svc = svc; }

    /*  Plans  */
    @PostMapping("/plans")
    public MembershipPlanDto create(@RequestBody PlanCreateReq req) { return svc.createPlan(req); }

    @GetMapping("/plans")
    public List<MembershipPlanDto> list() { return svc.listPlans(); }

    @PutMapping("/plans/{id}")
    public MembershipPlanDto update(@PathVariable Long id, @RequestBody PlanUpdateReq req) { return svc.updatePlan(id, req); }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { svc.deletePlan(id); return ResponseEntity.noContent().build(); }

    /*  Entitlements  */
    @PostMapping("/plans/{id}/entitlements")
    public MembershipEntitlementDto addEnt(@PathVariable Long id, @RequestBody EntitlementCreateReq req) {
        return svc.addEntitlement(id, req);
    }

    @PutMapping("/entitlements/{entId}")
    public MembershipEntitlementDto updEnt(@PathVariable Long entId, @RequestBody EntitlementUpdateReq req) {
        return svc.updateEntitlement(entId, req);
    }

    @DeleteMapping("/entitlements/{entId}")
    public ResponseEntity<Void> delEnt(@PathVariable Long entId) {
        svc.deleteEntitlement(entId);
        return ResponseEntity.noContent().build();
    }
}
