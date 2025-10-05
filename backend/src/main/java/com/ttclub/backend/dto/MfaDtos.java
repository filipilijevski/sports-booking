package com.ttclub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class MfaDtos {

    public record MfaSetupRequest(@NotBlank String password) { }
    public record MfaSetupResponse(String otpauthUrl, String maskedSecret, String qrDataUrl) { }

    public record MfaEnableRequest(@NotBlank String code) { }
    public record RecoveryCodesResponse(List<String> recoveryCodes) { }

    public record MfaDisableRequest(String password, String recoveryCode) { }

    public record MfaStatusResponse(boolean enabled) { }

    public record MfaChallengeDto(String status, String mfaToken, List<String> methods) { }

    public record MfaVerifyRequest(@NotBlank String mfaToken, @NotBlank String code) { }
}
