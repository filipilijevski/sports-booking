package com.ttclub.backend.controller;

import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {

    private final RoleService svc;

    public RoleController(RoleService svc) { this.svc = svc; }

    @PatchMapping("/{userId}")
    public ResponseEntity<Void> changeRole(
            @PathVariable long userId,
            @RequestParam RoleName newRole) {

        svc.changeRole(userId, newRole);
        return ResponseEntity.noContent().build();
    }
}
