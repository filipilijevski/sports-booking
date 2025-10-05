package com.ttclub.backend.dto;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,          // e.g., "CLIENT", "ADMIN"
        String provider,      // "LOCAL" or "GOOGLE"
        Instant createdAt
) { }
