package com.ttclub.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Globally enables {@code @PreAuthorize}/{@code @PostAuthorize} -
 * required by our custom {@code @RequiresRole} annotation.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class GlobalMethodSecurityConfig {
    /* empty on purpose */
}
