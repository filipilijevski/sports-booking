package com.ttclub.backend.dto;

import com.ttclub.backend.model.RoleName;

/** Payload used by an administrator to change somebody’s role. */
public record RoleChangeDto(RoleName newRole) { }
