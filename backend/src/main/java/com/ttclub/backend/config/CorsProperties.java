package com.ttclub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * Strong-typed binding for CORS settings.
 *
 * application.yml:<br/>
 *
 * ttclub:<br/>
 *   cors:<br/>
 *     allowed-origins:<br/>
 *       - http://localhost:5173<br/>
 */
@ConfigurationProperties(prefix = "ttclub.cors")
public class CorsProperties {

    /** When empty - no cross-origin allowed (recommended for prod) */
    private List<String> allowedOrigins = Collections.emptyList();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = (allowedOrigins == null)
                ? Collections.emptyList()
                : allowedOrigins;
    }
}
