package com.ttclub.backend.config;

import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwt;
    private final UserRepository users;

    private final boolean acceptBearerHeader;
    private final String accessCookieName;

    public JwtAuthFilter(JwtUtil jwt,
                         UserRepository users,
                         @Value("${ttclub.auth.accept-bearer-header:false}") boolean acceptBearerHeader,
                         @Value("${ttclub.cookies.access-name:access_token}") String accessCookieName) {
        this.jwt = jwt;
        this.users = users;
        this.acceptBearerHeader = acceptBearerHeader;
        this.accessCookieName = accessCookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String raw = null;

            if (acceptBearerHeader) {
                String hdr = req.getHeader("Authorization");
                if (hdr != null && hdr.startsWith("Bearer ")) {
                    raw = hdr.substring(7);
                }
            }

            if (raw == null) {
                Cookie[] cs = req.getCookies();
                if (cs != null) {
                    for (Cookie c : cs) {
                        if (accessCookieName.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                            raw = c.getValue();
                            break;
                        }
                    }
                }
            }

            if (raw != null && !raw.isBlank()) {
                try {
                    Claims claims = jwt.parse(raw).getBody();
                    String role = claims.get("role", String.class);
                    Long uid = Long.valueOf(claims.getSubject());
                    User principal = users.findById(uid).orElse(null);

                    List<SimpleGrantedAuthority> auths = new ArrayList<>();
                    auths.add(new SimpleGrantedAuthority("ROLE_" + role));
                    Boolean mustChange = claims.get("pwd_change_required", Boolean.class);
                    if (Boolean.TRUE.equals(mustChange)) {
                        auths.add(new SimpleGrantedAuthority("PWD_CHANGE_REQUIRED"));
                    }

                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, auths);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (JwtException | IllegalArgumentException ignored) {
                    // invalid / expired - proceed unauthenticated
                }
            }
        }

        chain.doFilter(req, res);
    }
}
