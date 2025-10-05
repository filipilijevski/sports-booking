package com.ttclub.backend.booking.dto;

import java.util.List;

/** Public-facing "card" used to render the Coaching page. */
public class ProgramCardDto {
    public Long id;
    public String title;
    public String description;
    public boolean active;
    public List<ProgramPackageDto> packages;
    /** Compressed weekly view (Mon - Sun with one or more time ranges) */
    public List<ProgramWeeklyDto> weekly;
    /** Distinct coach names across all slots */
    public List<String> coaches;
    /** Enrollment gating ("OPEN" | "ADMIN_ONLY") */
    public String enrollmentMode;
}
