package com.ttclub.backend.security;

import com.ttclub.backend.config.JwtUtil;
import com.ttclub.backend.model.RefreshToken;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.RefreshTokenRepository;
import com.ttclub.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwt;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenCookieService cookies;
    private final TokenHashingService hasher;
    private final String frontendUrl;

    /** 12h sliding TTL (seconds) injected from yml */
    private final long refreshTtlSec;

    /**
     * If true we include tokens in the redirect URL for legacy SPAs.
     * This must match ttclub.auth.redirect-include-tokens in application.yml.
     */
    private final boolean redirectIncludeTokens;

    public OAuth2SuccessHandler(
            JwtUtil jwt,
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            TokenCookieService cookies,
            TokenHashingService hasher,
            @Value("${ttclub.frontend-url:http://localhost:5173}") String feUrl,
            @Value("${ttclub.auth.refresh-ttl-sec:43200}") long refreshTtlSec,
            @Value("${ttclub.auth.redirect-include-tokens:false}") boolean redirectIncludeTokens
    ) {
        this.jwt                   = jwt;
        this.users                 = users;
        this.refreshTokens         = refreshTokens;
        this.cookies               = cookies;
        this.hasher                = hasher;
        this.refreshTtlSec         = refreshTtlSec;
        this.redirectIncludeTokens = redirectIncludeTokens;
        this.frontendUrl           = feUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication auth) throws IOException {

        OidcUser google = (OidcUser) auth.getPrincipal();
        String   email  = google.getEmail();

        /* 1 local user row is guaranteed (CustomOidcUserService) */
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User row missing"));

        /* 2 JWT + refresh-token (rotate-like on each OAuth2 login) */
        String access      = jwt.generate(user);
        String refreshRaw  = UUID.randomUUID().toString();
        String refreshHash = hasher.hash(refreshRaw);

        refreshTokens.save(new RefreshToken(
                refreshHash,
                user,
                Instant.now().plusSeconds(refreshTtlSec)));

        // Always set cookies (additive)
        cookies.writeAuthCookies(res, access, refreshRaw);

        /* 3 Return JSON for XHR callers, 302 for regular browsers */
        boolean wantsJson =
                "XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With")) ||
                        (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        if (wantsJson) {
            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("""
                {
                  "accessToken":  "%s",
                  "refreshToken": "%s",
                  "firstName":    "%s"
                }""".formatted(
                    access, refreshRaw,
                    user.getFirstName() == null ? "" : user.getFirstName()
            ));
            res.getWriter().flush();
        } else {
            final String target;
            if (redirectIncludeTokens) {
                // Legacy SPA expects tokens in the URL
                target = "%s/oauth2/callback?accessToken=%s&refreshToken=%s&firstName=%s"
                        .formatted(
                                frontendUrl,
                                URLEncoder.encode(access,  StandardCharsets.UTF_8),
                                URLEncoder.encode(refreshRaw, StandardCharsets.UTF_8),
                                URLEncoder.encode(
                                        user.getFirstName() == null ? "" : user.getFirstName(),
                                        StandardCharsets.UTF_8)
                        );
            } else {
                // Cookie-only: safer default with no tokens in the URL
                target = "%s/oauth2/callback".formatted(frontendUrl);
            }
            res.sendRedirect(target);
        }
    }
}
