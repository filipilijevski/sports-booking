package com.ttclub.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ttclub.backend.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Extended to allow registration code.
 * Backend validates all required fields. UI performs the same checks client-side.
 */
public record RegisterDto(
        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 8, max = 255)
        String password,

        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @JsonAlias({"role","requestedRole"})
        RoleName requestedRole,

        @JsonAlias({"code","registrationCode"})
        @Size(max = 255)
        String registrationCode,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits.")
        String verificationCode
) { }
