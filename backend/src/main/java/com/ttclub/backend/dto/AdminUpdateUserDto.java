package com.ttclub.backend.dto;

import com.ttclub.backend.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * All fields are optional; null means no change.
 * Admin can change name, and email (LOCAL only).
 * Admin can assign a different role.
 */
public record AdminUpdateUserDto(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        RoleName role
) { }
