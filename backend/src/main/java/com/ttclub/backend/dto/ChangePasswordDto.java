package com.ttclub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
        @NotBlank @Size(min = 8, max = 255) String newPassword,
        @NotBlank @Size(min = 8, max = 255) String confirmPassword
) { }
