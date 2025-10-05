package com.ttclub.backend.controller;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.service.AuthService;
import com.ttclub.backend.security.TokenCookieService;
import com.ttclub.backend.service.MfaService;
import com.ttclub.backend.model.User;
import com.ttclub.backend.service.RateLimitService;
import com.ttclub.backend.config.AuthRateLimitProperties; // <— new
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService svc;
    private final MfaService mfa;
    private final TokenCookieService cookies;
    private final RateLimitService rateLimit;
    private final AuthRateLimitProperties rlProps; // <— new

    public AuthController(AuthService svc, MfaService mfa, TokenCookieService cookies,
                          RateLimitService rateLimit, AuthRateLimitProperties rlProps) {
        this.svc = svc;
        this.mfa = mfa;
        this.cookies = cookies;
        this.rateLimit = rateLimit;
        this.rlProps = rlProps; // <— new
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponseDto register(@RequestBody @Valid RegisterDto dto,
                                     HttpServletResponse res) {
        LoginResponseDto rsp = (LoginResponseDto) svc.register(dto);
        cookies.writeAuthCookies(res, rsp.accessToken(), rsp.refreshToken());
        return rsp;
    }

    @PostMapping("/login")
    public Object login(@RequestBody LoginDto dto,
                        HttpServletRequest req,
                        HttpServletResponse res) {
        // configurable: login limit/window
        rateLimit.check("auth:login",
                clientIp(req) + ":" + normalize(dto.email()),
                rlProps.getLogin().getLimit(),
                rlProps.getLogin().getWindowSec());

        Object rsp = svc.login(dto);
        if (rsp instanceof LoginResponseDto tokens) {
            cookies.writeAuthCookies(res, tokens.accessToken(), tokens.refreshToken());
            return tokens;
        }
        return rsp; // MFA_REQUIRED - do not set cookies
    }

    @PostMapping("/mfa/verify")
    public LoginResponseDto verify(@RequestBody @Valid MfaDtos.MfaVerifyRequest body,
                                   HttpServletRequest req,
                                   HttpServletResponse res) {
        // configurable: mfa verify limit/window
        rateLimit.check("auth:mfa:verify",
                clientIp(req) + ":" + body.mfaToken(),
                rlProps.getMfaVerify().getLimit(),
                rlProps.getMfaVerify().getWindowSec());

        MfaService.MfaVerifyResult vr = Objects.requireNonNull(
                mfa.verifyPreAuthAndCodeDetailed(body.mfaToken(), body.code()),
                "MFA verification failed"
        );

        boolean mustChange = vr.pwdChangeRequired();
        User u = vr.user();
        LoginResponseDto tokens = mustChange
                ? svc.issueTokensForUser(u, Map.of("pwd_change_required", true))
                : svc.issueTokensForUser(u);
        cookies.writeAuthCookies(res, tokens.accessToken(), tokens.refreshToken());
        return tokens;
    }

    @PostMapping("/send-code")
    @ResponseStatus(HttpStatus.OK)
    public void sendCode(@RequestBody Map<String, String> payload,
                         HttpServletRequest req) {
        String email = payload.get("email");
        String device = readCookie(req, "device_id");
        // configurable: send-code limit/window
        rateLimit.check("auth:send-code",
                clientIp(req) + ":" + normalize(email) + ":" + (device == null ? "" : device),
                rlProps.getSendCode().getLimit(),
                rlProps.getSendCode().getWindowSec());
        svc.sendVerificationCode(email);
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.OK)
    public void passwordReset(@RequestBody @Valid PasswordResetRequestDto body,
                              HttpServletRequest req) {
        // configurable: password reset request limit/window
        rateLimit.check("auth:pwd-reset",
                clientIp(req) + ":" + normalize(body.email()),
                rlProps.getPasswordReset().getLimit(),
                rlProps.getPasswordReset().getWindowSec());
        svc.requestPasswordReset(body.email());
    }

    @PostMapping("/refresh")
    public LoginResponseDto refresh(@RequestBody(required = false) TokenPairDto body,
                                    HttpServletRequest req,
                                    HttpServletResponse res) {
        String rt = (body != null && body.refreshToken() != null && !body.refreshToken().isBlank())
                ? body.refreshToken()
                : cookies.readRefreshFromCookie(req);

        if (rt == null || rt.isBlank()) {
            throw new IllegalArgumentException("Invalid refreshToken");
        }

        LoginResponseDto rsp = svc.refresh(rt);
        cookies.writeAuthCookies(res, rsp.accessToken(), rsp.refreshToken());
        return rsp;
    }

    @GetMapping("/session")
    public AuthSessionDto session() {
        var a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean must =
                a != null && a.getAuthorities().stream()
                        .anyMatch(g -> "PWD_CHANGE_REQUIRED".equals(g.getAuthority()));
        return new AuthSessionDto(must);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) TokenPairDto body,
                       HttpServletRequest req,
                       HttpServletResponse res) {
        String rt = (body != null && body.refreshToken() != null && !body.refreshToken().isBlank())
                ? body.refreshToken()
                : cookies.readRefreshFromCookie(req);

        if (rt != null && !rt.isBlank()) svc.logout(rt);
        cookies.clearAuthCookies(res);
    }

    private static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        return xf != null && !xf.isBlank() ? xf.split(",")[0].trim() : req.getRemoteAddr();
    }
    private static String normalize(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
    private static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }
}
