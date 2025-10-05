package com.ttclub.backend.security.filters;

import com.ttclub.backend.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Path-pattern rate limits for payment-intent & coupon validate endpoints
 * (public surface). Auth-specific endpoints are limited in controllers/services.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rl;

    public RateLimitFilter(RateLimitService rl) {
        this.rl = rl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = req.getRequestURI();
        String ip = clientIp(req);
        String device = readCookie(req, "device_id");
        String userKey = (String.valueOf(req.getUserPrincipal()) + ":" + (device == null ? "anon" : device));

        try {
            if (uri.contains("/programs/") && uri.contains("payment-intent")) {
                rl.check("programs:payment-intent", ip + ":" + userKey, 10, 600);
            } else if (uri.contains("/table-credits/") && uri.contains("payment-intent")) {
                rl.check("table-credits:payment-intent", ip + ":" + userKey, 10, 600);
            } else if (uri.contains("coupon") && uri.contains("validate")) {
                rl.check("coupon:validate", ip, 15, 600);
            }
            chain.doFilter(req, res);
        } catch (com.ttclub.backend.service.exceptions.RateLimitedException ex) {
            res.setStatus(429);
            res.setContentType("application/json;charset=UTF-8");
            String json = """
              {"code":"RATE_LIMITED","message":"Too many requests. Try again later.","retryAfterSec":%d}
              """.formatted(ex.getRetryAfterSec());
            res.getWriter().write(json);
            res.getWriter().flush();
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        return xf != null && !xf.isBlank() ? xf.split(",")[0].trim() : req.getRemoteAddr();
    }

    private static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }
}
