package com.ttclub.backend.booking.dto;

/** Payload for creating a membership plan. */
public class PlanCreateReq {
    /** INITIAL | SPECIAL */
    public String type;
    public String name;
    public Double priceCad;
    public Integer durationDays;
    public Boolean active;
    public String description;
    /** INDIVIDUAL | GROUP (default INDIVIDUAL) */
    public String holderKind; 
}
