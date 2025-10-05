package com.ttclub.backend.booking.job;

import com.ttclub.backend.booking.service.ProgramOccurrenceService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly materialiser for program occurrences.
 * Generates future occurrences from weekly ProgramSlot templates.
 * - Enabled by default; can be disabled via:
 *     jobs.program-materialiser.enabled=false
 * - Cron can be changed via:
 *     jobs.program-materialiser.cron=...
 */
@Component
@ConditionalOnProperty(
        name = "jobs.program-materialiser.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ProgramMaterialiserJob {

    private static final Logger log = LoggerFactory.getLogger(ProgramMaterialiserJob.class);

    private final ProgramOccurrenceService svc;

    @Value("${jobs.program-materialiser.cron:0 10 2 * * *}")
    private String cron;

    // Optional: set a scheduling zone (defaults to JVM zone if empty)
    @Value("${jobs.program-materialiser.zone:}")
    private String zone;

    public ProgramMaterialiserJob(ProgramOccurrenceService svc) {
        this.svc = svc;
    }

    @PostConstruct
    void init() {
        if (zone == null || zone.isBlank()) {
            log.info("ProgramMaterialiserJob enabled. Cron: [{}] (JVM default zone)", cron);
        } else {
            log.info("ProgramMaterialiserJob enabled. Cron: [{}], Zone: [{}]", cron, zone);
        }
    }

    // second, minute, hour, day, month, day-of-week
    @Scheduled(cron = "${jobs.program-materialiser.cron:0 10 2 * * *}",
            zone = "${jobs.program-materialiser.zone:}")
    public void runNightly() {
        int created = svc.materializeDefaultWindow();
        log.info("ProgramMaterialiserJob executed: created {} new occurrences.", created);
    }
}
