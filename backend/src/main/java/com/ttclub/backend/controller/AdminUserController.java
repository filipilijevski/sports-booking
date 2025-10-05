package com.ttclub.backend.controller;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.model.AuthProvider;
import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.service.UserService;
import com.ttclub.backend.service.MfaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only endpoints to manage user profiles.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class AdminUserController {

    private final UserService svc;
    private final MfaService mfaSvc;

    public AdminUserController(UserService svc, MfaService mfaSvc) {
        this.svc = svc;
        this.mfaSvc = mfaSvc;
    }

    @GetMapping
    public Page<AdminUserDto> search(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) RoleName role,
                                     @RequestParam(required = false) AuthProvider provider,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "12") int size) {
        return svc.adminSearch(q, role, provider, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserDto create(@RequestBody @Valid AdminCreateUserDto body) {
        return svc.adminCreate(body);
    }

    @PatchMapping("/{id}")
    public AdminUserDto update(@PathVariable long id,
                               @RequestBody @Valid AdminUpdateUserDto body) {
        return svc.adminUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@AuthenticationPrincipal com.ttclub.backend.model.User current,
                           @PathVariable long id) {
        svc.adminDelete(current.getId(), id);
    }

    @PostMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable long id,
                              @RequestBody @Valid ResetPasswordAdminDto body) {
        svc.adminResetPassword(id, body.temporaryPassword());
    }

    @PostMapping("/{id}/mfa/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminDisableMfa(@PathVariable long id) {
        mfaSvc.adminDisable(id);
    }
}
