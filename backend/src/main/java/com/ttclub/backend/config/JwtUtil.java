package com.ttclub.backend.config;

import com.ttclub.backend.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * Stateless HS-256 JWT helper.
 */
@Component
public class JwtUtil {

    private final Key    key;
    private final long   ttlMs;
    private final String issuer;
    private final String audience;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.ttl-minutes:60}") long ttlMinutes,
                   @Value("${jwt.issuer}") String issuer,
                   @Value("${jwt.audience}") String audience) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Missing JWT secret - set TTCLUB_JWT_SECRET / jwt.secret");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters for HS-256");
        }
        this.key      = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMs    = ttlMinutes * 60 * 1_000L;
        this.issuer   = issuer;
        this.audience = audience;
    }

    public long getTtlSeconds() {
        return Math.max(0, ttlMs / 1000L);
    }

    public String generate(User user) {
        return generate(user, Map.of());
    }

    /** Overload to embed optional claims (e.g. pwd_change_required). */
    public String generate(User user, Map<String, Object> extraClaims) {
        Date now = new Date();
        JwtBuilder b = Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("role", user.getRole().getName().name())
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMs))
                .signWith(key, SignatureAlgorithm.HS256);

        if (extraClaims != null) {
            for (var e : extraClaims.entrySet()) {
                b.claim(e.getKey(), e.getValue());
            }
        }
        return b.compact();
    }

    public Jws<Claims> parse(String raw) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(30)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(raw);
    }
}
