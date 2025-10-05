package com.ttclub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordAdminDto(
        @NotBlank @Size(min = 8, max = 255) String temporaryPassword
) { }
