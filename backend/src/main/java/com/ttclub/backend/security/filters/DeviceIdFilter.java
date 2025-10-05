package com.ttclub.backend.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Issues a sticky, non-HttpOnly device_id cookie for anonymous correlation. */
@Component
public class DeviceIdFilter extends OncePerRequestFilter {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final String domain;
    private final String path;

    public DeviceIdFilter(
            @Value("${ttclub.cookies.device-name:device_id}") String cookieName,
            @Value("${ttclub.cookies.secure:false}") boolean secure,
            @Value("${ttclub.cookies.same-site:Lax}") String sameSite,
            @Value("${ttclub.cookies.domain:}") String domain,
            @Value("${ttclub.cookies.path:/}") String path
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.domain = domain;
        this.path = path;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        if (!hasCookie(req, cookieName)) {
            String val = UUID.randomUUID().toString();
            ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(cookieName, val)
                    .httpOnly(false)
                    .secure(secure)
                    .sameSite(sameSite)
                    .path(path)
                    .maxAge(60L * 60L * 24L * 365L); // 1 year
            if (domain != null && !domain.isBlank()) b = b.domain(domain);
            res.addHeader("Set-Cookie", b.build().toString());
        }
        chain.doFilter(req, res);
    }

    private static boolean hasCookie(HttpServletRequest req, String name) {
        Cookie[] c = req.getCookies();
        if (c == null) return false;
        for (Cookie it : c) if (name.equals(it.getName())) return true;
        return false;
    }
}
