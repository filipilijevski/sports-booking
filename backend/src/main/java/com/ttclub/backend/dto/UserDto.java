package com.ttclub.backend.dto;

public record UserDto(
        Long id,
        String email,
        String role,
        String firstName,
        String lastName,
        String provider,
        boolean mfaEnabled
) { }
