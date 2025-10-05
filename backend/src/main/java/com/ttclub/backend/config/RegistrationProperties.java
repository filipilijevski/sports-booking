package com.ttclub.backend.config;

import com.ttclub.backend.model.RoleName;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Binds <tt>ttclub.registration.codes.*</tt> from <i>application.yml</i>
 * so {@link com.ttclub.backend.service.AuthService} can validate
 * self‑registration role requests. Not currently implemented on the frontend.
 */
@Component
@ConfigurationProperties(prefix = "ttclub.registration")
public class RegistrationProperties {

    /**
     * Map of ROLE to secret phrase.
     * Leave a role absent (or value empty) if it should never be self‑assigned.
     */
    private Map<@NotNull RoleName, String> codes = Map.of();

    public Map<RoleName, String> getCodes() { return codes; }
    public void setCodes(Map<RoleName, String> c) { this.codes = c; }
}
