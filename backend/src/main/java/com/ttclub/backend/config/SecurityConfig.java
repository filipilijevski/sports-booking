package com.ttclub.backend.config;

import com.ttclub.backend.security.CustomOidcUserService;
import com.ttclub.backend.security.OAuth2SuccessHandler;
import com.ttclub.backend.security.CsrfCookieFilter;
import com.ttclub.backend.security.filters.DeviceIdFilter;
import com.ttclub.backend.security.filters.PasswordChangeRequiredFilter;
import com.ttclub.backend.security.filters.RateLimitFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring-Security configuration.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    /* JSON API - JWT only */
    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http,
                                 JwtAuthFilter jwt,
                                 CsrfCookieFilter csrf,
                                 DeviceIdFilter device,
                                 RateLimitFilter rateLimit,
                                 PasswordChangeRequiredFilter pwdGate,
                                 @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {

        http.securityMatcher("/api/**")
                .cors(c -> c.configurationSource(cors))
                .csrf(cs -> cs.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        /* public read-only */
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**", "/api/memberships/plans", "/api/table-credits/**", "/api/blog/**").permitAll()

                        /* auth endpoints */
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/programs/**").permitAll()

                        /* guest checkout */
                        .requestMatchers(HttpMethod.POST, "/api/guest/checkout").permitAll()

                        /* allow anonymous shipping quotes (guests) and stock */
                        .requestMatchers(HttpMethod.POST, "/api/shipping/quote").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/stock/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/shipping/quote/details").permitAll()

                        /* Stripe webhooks (public, no JWT) */
                        .requestMatchers(HttpMethod.POST, "/api/stripe/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/stripe").permitAll()

                        /* everything else needs JWT */
                        .anyRequest().authenticated())
                .oauth2Login(ol -> ol.disable())
                .addFilterBefore(device,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(csrf,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimit,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwt,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(pwdGate, JwtAuthFilter.class);

        return http.build();
    }

    /* Browser chain */
    @Bean
    @Order(2)
    SecurityFilterChain browserChain(HttpSecurity http,
                                     CustomOidcUserService oidcSvc,
                                     OAuth2SuccessHandler successHandler,
                                     CsrfCookieFilter csrf,
                                     DeviceIdFilter device,
                                     RateLimitFilter rateLimit,
                                     @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {

        http.securityMatcher("/**")   // /api/** captured above
                .cors(c -> c.configurationSource(cors))
                .csrf(cs -> cs.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/actuator/health",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(ui -> ui.oidcUserService(oidcSvc))
                        .successHandler(successHandler))
                .addFilterBefore(device,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(csrf,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimit,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /* password encoder */
    @Bean public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* CORS (property-driven) */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
        final CorsConfiguration cfg = buildCorsConfiguration(props.getAllowedOrigins());

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    /** Small helper to keep things concise. */
    private static CorsConfiguration buildCorsConfiguration(List<String> allowedOrigins) {
        CorsConfiguration cfg = new CorsConfiguration();

        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            cfg.setAllowedOriginPatterns(List.of());
            cfg.setAllowCredentials(false);
        } else {
            cfg.setAllowedOriginPatterns(allowedOrigins);
            cfg.setAllowCredentials(true);
        }

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF", "X-Requested-With"));
        cfg.setMaxAge(3600L);
        return cfg;
    }
}
