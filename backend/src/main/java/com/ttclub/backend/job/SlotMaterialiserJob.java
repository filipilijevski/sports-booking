package com.ttclub.backend.job;

import com.ttclub.backend.model.ScheduleTemplate;
import com.ttclub.backend.model.TimeSlot;
import com.ttclub.backend.repository.ScheduleTemplateRepository;
import com.ttclub.backend.repository.TimeSlotRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * LEGACY: Slot-based materialiser.
 * This component is now disabled by default and superseded by ProgramMaterialiserJob.
 */
@Deprecated(forRemoval = false)
@Component
@ConditionalOnProperty(
        name = "jobs.legacy.slot-materialiser.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SlotMaterialiserJob {

    private static final Logger log = LoggerFactory.getLogger(SlotMaterialiserJob.class);

    private final ScheduleTemplateRepository templates;
    private final TimeSlotRepository         slots;

    public SlotMaterialiserJob(ScheduleTemplateRepository templates,
                               TimeSlotRepository slots) {
        this.templates = templates; this.slots = slots;
    }

    /* make sure the scheduler is enabled (if we need it for any reason) */
    @PostConstruct
    void init() { log.warn("LEGACY SlotMaterialiserJob is ENABLED via property and will run on schedule."); }

    /**
     * Runs every night at 02:00 - creates concrete slots for the next 14 days.
     * NOTE: This job is legacy and should remain disabled in normal operation.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void run() {
        LocalDate today     = LocalDate.now();
        LocalDate horizon   = today.plusDays(14);

        List<ScheduleTemplate> active = templates.findByActiveTrue();
        active.forEach(tpl -> materialiseTemplate(tpl, today, horizon));
    }

    private void materialiseTemplate(ScheduleTemplate tpl,
                                     LocalDate from, LocalDate to) {

        LocalDate day = from.with(TemporalAdjusters.nextOrSame(tpl.getWeekday()));
        while (!day.isAfter(to)) {
            LocalDateTime start = LocalDateTime.of(day, tpl.getStartTime());
            if (!slots.existsByCoachIdAndStartTime(tpl.getCoach().getId(), start)) {
                TimeSlot s = new TimeSlot();
                s.setCoach(tpl.getCoach());
                s.setStartTime(start);
                s.setEndTime(LocalDateTime.of(day, tpl.getEndTime()));
                s.setCapacity(tpl.getCapacity());
                s.setPrice(tpl.getPrice());
                slots.save(s);
            }
            day = day.plusWeeks(1);   // next occurrence
        }
    }
}
