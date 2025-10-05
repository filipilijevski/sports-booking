package com.ttclub.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Central switch for Spring's @Scheduled support.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    @PostConstruct
    void init() {
        log.info("Scheduling enabled (@EnableScheduling active).");
    }
}
