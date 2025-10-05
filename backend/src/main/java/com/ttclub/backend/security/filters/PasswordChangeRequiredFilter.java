package com.ttclub.backend.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * If the JWT carries claim pwd_change_required=true, we gate all API calls
 * except password change, logout/refresh, and /users/me & /auth/session probes.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean require = a != null && a.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("PWD_CHANGE_REQUIRED"::equals);

        if (!require) {
            chain.doFilter(req, res);
            return;
        }

        final String uri = req.getRequestURI();
        final String method = req.getMethod();

        boolean allowed =
                ("/api/users/me/password".equals(uri) && "POST".equalsIgnoreCase(method)) ||
                        ("/api/auth/logout".equals(uri)       && "POST".equalsIgnoreCase(method)) ||
                        ("/api/auth/refresh".equals(uri)      && "POST".equalsIgnoreCase(method)) ||
                        ("/api/users/me".equals(uri)          && "GET".equalsIgnoreCase(method))  ||
                        ("/api/auth/session".equals(uri)      && "GET".equalsIgnoreCase(method));

        if (allowed) {
            chain.doFilter(req, res);
            return;
        }

        res.setStatus(428); // Precondition Required
        res.setContentType("application/json;charset=UTF-8");
        String json = "{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"You must change your password before continuing.\",\"fieldErrors\":{}}";
        res.getWriter().write(json);
        res.getWriter().flush();
    }
}
