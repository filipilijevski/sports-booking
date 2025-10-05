package com.ttclub.backend.controller;

import com.ttclub.backend.dto.ChangePasswordDto;
import com.ttclub.backend.dto.UpdateProfileDto;
import com.ttclub.backend.dto.UserDto;
import com.ttclub.backend.mapper.UserMapper;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes “/api/users/me” so that the front‐end can fetch and update
 * the logged‐in user’s profile.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper mapper;
    private final UserService svc;

    public UserController(UserMapper mapper, UserService svc) {
        this.mapper = mapper;
        this.svc    = svc;
    }

    /**
     * GET /api/users/me
     * Returns the current user's profile.
     */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal User user) {
        return mapper.toDto(user);
    }

    /**
     * PATCH /api/users/me
     * Partially update firstName / lastName / email (LOCAL only for email).
     */
    @PatchMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal User user,
                            @RequestBody @Valid UpdateProfileDto dto) {
        return svc.updateOwn(user.getId(), dto);
    }

    /**
     * POST /api/users/me/password
     * Change password (LOCAL only).
     */
    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal User user,
                               @RequestBody @Valid ChangePasswordDto body) {
        svc.changePasswordOwn(user.getId(), body);
    }
}
