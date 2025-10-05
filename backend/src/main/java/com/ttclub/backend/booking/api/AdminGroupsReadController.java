package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.AdminGroupDtos.GroupDetail;
import com.ttclub.backend.booking.dto.AdminGroupDtos.GroupListItem;
import com.ttclub.backend.booking.service.AdminGroupQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/memberships/groups")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminGroupsReadController {

    private final AdminGroupQueryService svc;

    public AdminGroupsReadController(AdminGroupQueryService svc) { this.svc = svc; }

    @GetMapping
    public List<GroupListItem> list() { return svc.list(); }

    @GetMapping("/{id}")
    public GroupDetail get(@PathVariable Long id) { return svc.get(id); }
}
