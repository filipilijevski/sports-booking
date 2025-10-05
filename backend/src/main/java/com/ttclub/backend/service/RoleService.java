package com.ttclub.backend.service;

import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.repository.RoleRepository;
import com.ttclub.backend.repository.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleService {

    private final UserRepository users;
    private final RoleRepository roles;

    public RoleService(UserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    /**
     * Change a user’s role.  Allowed only for ADMIN and only if the user has no future lessons/bookings
     * (will wire real checks when scheduling is ready).
     */
    @RolesAllowed("ADMIN")
    public void changeRole(long userId, RoleName newRole) {
        var user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(
                roles.findByName(newRole)
                        .orElseThrow(() -> new IllegalStateException("Role row missing: " + newRole))
        );
    }
}
