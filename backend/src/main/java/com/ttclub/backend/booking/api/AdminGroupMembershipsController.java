package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.model.MembershipGroup;
import com.ttclub.backend.booking.model.MembershipPlan;
import com.ttclub.backend.booking.model.UserMembership;
import com.ttclub.backend.booking.repository.MembershipGroupRepository;
import com.ttclub.backend.booking.repository.MembershipPlanRepository;
import com.ttclub.backend.booking.repository.UserMembershipRepository;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/memberships")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminGroupMembershipsController {

    private final MembershipGroupRepository groups;
    private final UserRepository users;
    private final UserMembershipRepository userMemberships;
    private final MembershipPlanRepository plans;

    public AdminGroupMembershipsController(MembershipGroupRepository groups,
                                           UserRepository users,
                                           UserMembershipRepository userMemberships,
                                           MembershipPlanRepository plans) {
        this.groups = groups;
        this.users = users;
        this.userMemberships = userMemberships;
        this.plans = plans;
    }

    public record AddMemberReq(Long userId) {}
    public record DeactivateResp(boolean ok) {}

    /** Add a user to an existing group membership (shares same plan dates). */
    @PostMapping("/groups/{groupId}/members")
    @Transactional
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestBody AddMemberReq req) {
        MembershipGroup g = groups.findById(groupId).orElseThrow();
        if (!Boolean.TRUE.equals(g.getActive())) return ResponseEntity.badRequest().body("Group is inactive.");

        User member = users.findById(req.userId()).orElseThrow();

        // prevent duplicate active record for same plan concurrently
        if (userMemberships.existsActiveForUserAndPlan(member.getId(), g.getPlan().getId(), Instant.now())) {
            return ResponseEntity.badRequest().body("User already has an active membership for this plan.");
        }

        UserMembership um = new UserMembership();
        um.setUser(member);
        um.setPlan(g.getPlan());
        um.setGroup(g);
        um.setStartTs(g.getStartTs());
        um.setEndTs(g.getEndTs());
        um.setActive(true);
        userMemberships.save(um);

        return ResponseEntity.ok().build();
    }

    /** Deactivate a specific user's membership (individual or group-linked). */
    @PostMapping("/user-memberships/{id}/deactivate")
    @Transactional
    public DeactivateResp deactivateUserMembership(@PathVariable Long id) {
        UserMembership um = userMemberships.findById(id).orElseThrow();
        um.setActive(false);
        userMemberships.save(um);
        return new DeactivateResp(true);
    }

    /** Deactivate a whole group (and cascade to its members). */
    @PostMapping("/groups/{groupId}/deactivate")
    @Transactional
    public DeactivateResp deactivateGroup(@PathVariable Long groupId) {
        MembershipGroup g = groups.findById(groupId).orElseThrow();
        g.setActive(false);
        groups.save(g);

        // set all user_memberships with this group to inactive
        userMemberships.findAll().stream()
                .filter(um -> um.getGroup() != null && um.getGroup().getId().equals(groupId))
                .forEach(um -> { um.setActive(false); userMemberships.save(um); });

        return new DeactivateResp(true);
    }
}
