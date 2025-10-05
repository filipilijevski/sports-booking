package com.ttclub.backend.booking.dto;

import java.util.List;

/** Program aggregate with packages and weekly slots (admin/editor views) */
public class ProgramDto {
    public Long id;
    public String title;
    public String description;
    public boolean active;
    public List<ProgramPackageDto> packages;
    public List<ProgramSlotDto> slots;
    /** Enrollment gating ("OPEN" | "ADMIN_ONLY") */
    public String enrollmentMode;
}
