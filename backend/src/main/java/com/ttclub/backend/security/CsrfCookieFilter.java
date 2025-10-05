package com.ttclub.backend.security;

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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Double-submit CSRF:
 *  - Issues a non-HttpOnly session cookie "XSRF-TOKEN".<br>
 *  - Requires header "X-CSRF" to match the cookie on POST/PUT/PATCH/DELETE
 *    only when auth cookies (access or refresh) are present.<br>
 *  - Skips for Authorization: Bearer (legacy header mode), safe methods, and exempt paths.<br>
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final SecureRandom RNG = new SecureRandom();

    private final boolean enabled;
    private final String cookieName;
    private final String headerName;

    private final boolean secure;
    private final String sameSite;
    private final String domain;
    private final String path;
    private final String accessCookieName;
    private final String refreshCookieName;

    private final Set<String> exemptPrefixes = Set.of(
            "/api/stripe/", "/api/webhooks/stripe", "/uploads/"
    );

    public CsrfCookieFilter(
            @Value("${ttclub.csrf.enabled:true}") boolean enabled,
            @Value("${ttclub.csrf.cookie-name:XSRF-TOKEN}") String cookieName,
            @Value("${ttclub.csrf.header-name:X-CSRF}") String headerName,
            @Value("${ttclub.cookies.secure:false}") boolean secure,
            @Value("${ttclub.cookies.same-site:Lax}") String sameSite,
            @Value("${ttclub.cookies.domain:}") String domain,
            @Value("${ttclub.cookies.path:/}") String path,
            @Value("${ttclub.cookies.access-name:access_token}") String accessCookieName,
            @Value("${ttclub.cookies.refresh-name:refresh_token}") String refreshCookieName
    ) {
        this.enabled = enabled;
        this.cookieName = cookieName;
        this.headerName = headerName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.domain = domain;
        this.path = path;
        this.accessCookieName = accessCookieName;
        this.refreshCookieName = refreshCookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!enabled) {
            chain.doFilter(req, res);
            return;
        }

        ensureCookiePresent(req, res);

        final String method = req.getMethod();
        final String uri = req.getRequestURI();

        if (isSafeMethod(method) || isExemptPath(uri) || hasBearerAuth(req) || !hasAuthCookies(req)) {
            chain.doFilter(req, res);
            return;
        }

        final String headerVal = req.getHeader(headerName);
        final String cookieVal = readCookie(req, cookieName);

        if (cookieVal == null || headerVal == null || !constantTimeEquals(cookieVal, headerVal)) {
            writeCsrfError(res);
            return;
        }

        chain.doFilter(req, res);
    }

    private void ensureCookiePresent(HttpServletRequest req, HttpServletResponse res) {
        String val = readCookie(req, cookieName);
        if (val != null && !val.isBlank()) return;

        String token = newToken();
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(cookieName, token)
                .httpOnly(false)
                .secure(secure)
                .sameSite(sameSite)
                .path(path);
        if (domain != null && !domain.isBlank()) b = b.domain(domain);
        res.addHeader("Set-Cookie", b.build().toString());
    }

    private boolean isSafeMethod(String m) {
        return "GET".equalsIgnoreCase(m) || "HEAD".equalsIgnoreCase(m) || "OPTIONS".equalsIgnoreCase(m);
    }

    private boolean hasBearerAuth(HttpServletRequest req) {
        String hdr = req.getHeader("Authorization");
        return hdr != null && hdr.startsWith("Bearer ");
    }

    private boolean hasAuthCookies(HttpServletRequest req) {
        Cookie[] c = req.getCookies();
        if (c == null) return false;
        boolean hasAccess = false;
        boolean hasRefresh = false;
        for (Cookie it : c) {
            if (accessCookieName.equals(it.getName()) && it.getValue() != null && !it.getValue().isBlank()) hasAccess = true;
            if (refreshCookieName.equals(it.getName()) && it.getValue() != null && !it.getValue().isBlank()) hasRefresh = true;
        }
        return hasAccess || hasRefresh;
    }

    private boolean isExemptPath(String path) {
        if (path == null) return false;
        for (String p : exemptPrefixes) if (path.startsWith(p)) return true;
        return false;
    }

    private static String newToken() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String readCookie(HttpServletRequest req, String name) {
        Cookie[] cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes();
        byte[] y = b.getBytes();
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= (x[i] ^ y[i]);
        return r == 0;
    }

    private void writeCsrfError(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        String json = "{\"code\":\"CSRF_INVALID\",\"message\":\"Cross-site request forgery check failed.\",\"fieldErrors\":{}}";
        res.getWriter().write(json);
        res.getWriter().flush();
    }
}
