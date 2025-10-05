package com.ttclub.backend.booking.dto;

import java.util.List;

/** Public DTO used in API responses. */
public class MembershipPlanDto {
    public Long id;
    /** INITIAL | SPECIAL */
    public String type;
    /** INDIVIDUAL | GROUP */
    public String holderKind; 
    public String name;
    public Double priceCad;
    public String description;
    /** Plan length in days. INITIAL must be time-bound (typically 365). */
    public Integer durationDays;
    public boolean active;
    public List<MembershipEntitlementDto> entitlements;
}
