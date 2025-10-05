package com.ttclub.backend.config;

import com.ttclub.backend.model.RoleName;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ttclub.registration")
public class RegistrationCodeConfig {

    /** role to secret code */
    private final Map<RoleName, String> codes = new EnumMap<>(RoleName.class);

    public Map<RoleName, String> getCodes() { return codes; }

    /** @return the role whose code matches (null if none / blank) */
    public RoleName resolve(String supplied) {
        if (supplied == null || supplied.isBlank()) return null;
        return codes.entrySet().stream()
                .filter(e -> e.getValue().equals(supplied))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
