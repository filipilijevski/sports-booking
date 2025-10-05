package com.ttclub.backend.security;

import com.ttclub.backend.config.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Component
public class TokenCookieService {

    private final JwtUtil jwt;
    private final boolean secure;
    private final String  sameSite;
    private final String  domain;
    private final String  path;
    private final String  accessName;
    private final String  refreshName;
    private final long    refreshTtlSec;

    public TokenCookieService(
            JwtUtil jwt,
            @Value("${ttclub.cookies.secure:false}") boolean secure,
            @Value("${ttclub.cookies.same-site:Lax}") String sameSite,
            @Value("${ttclub.cookies.domain:}") String domain,
            @Value("${ttclub.cookies.path:/}") String path,
            @Value("${ttclub.cookies.access-name:access_token}") String accessName,
            @Value("${ttclub.cookies.refresh-name:refresh_token}") String refreshName,
            @Value("${ttclub.auth.refresh-ttl-sec:43200}") long refreshTtlSec
    ) {
        this.jwt           = jwt;
        this.secure        = secure;
        this.sameSite      = sameSite;
        this.domain        = domain;
        this.path          = path;
        this.accessName    = accessName;
        this.refreshName   = refreshName;
        this.refreshTtlSec = refreshTtlSec;
    }

    /** Issue both cookies (HttpOnly). */
    public void writeAuthCookies(HttpServletResponse res,
                                 String accessToken,
                                 String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            addCookie(res, accessName, accessToken, jwt.getTtlSeconds(), true);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            addCookie(res, refreshName, refreshToken, refreshTtlSec, true);
        }
    }

    /** Delete both cookies (idempotent). */
    public void clearAuthCookies(HttpServletResponse res) {
        addCookie(res, accessName,  "", 0, true);
        addCookie(res, refreshName, "", 0, true);
    }

    /** Return the refresh token from cookie (or null if absent). */
    public String readRefreshFromCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (var c : req.getCookies()) {
            if (refreshName.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void addCookie(HttpServletResponse res,
                           String name,
                           String value,
                           long maxAgeSeconds,
                           boolean httpOnly) {

        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .path(path);

        if (maxAgeSeconds >= 0) {
            b = b.maxAge(Duration.ofSeconds(maxAgeSeconds));
        }
        if (domain != null && !domain.isBlank()) {
            b = b.domain(domain);
        }

        res.addHeader("Set-Cookie", b.build().toString());
    }
}
