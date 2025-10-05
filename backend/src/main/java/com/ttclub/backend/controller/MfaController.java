package com.ttclub.backend.controller;

import com.ttclub.backend.dto.MfaDtos.*;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.MfaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final MfaService svc;

    public MfaController(MfaService svc) { this.svc = svc; }

    @PostMapping("/setup")
    public MfaSetupResponse setup(@AuthenticationPrincipal User user,
                                  @RequestBody @Valid MfaSetupRequest body) {
        var r = svc.setup(user, body.password());
        return new MfaSetupResponse(r.otpauthUrl(), r.maskedSecret(), r.qrDataUrl());
        // Frontend will show QR and ask for TOTP entry to confirm/enable
    }

    @PostMapping("/enable")
    public RecoveryCodesResponse enable(@AuthenticationPrincipal User user,
                                        @RequestBody @Valid MfaEnableRequest body) {
        var codes = svc.enable(user, body.code());
        return new RecoveryCodesResponse(codes);
    }

    @PostMapping("/disable")
    public void disable(@AuthenticationPrincipal User user,
                        @RequestBody @Valid MfaDisableRequest body) {
        svc.disable(user, body.password(), body.recoveryCode());
    }

    @GetMapping("/status")
    public MfaStatusResponse status(@AuthenticationPrincipal User user) {
        return new MfaStatusResponse(Boolean.TRUE.equals(user.getMfaEnabled()));
    }
}
