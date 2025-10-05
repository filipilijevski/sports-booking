package com.ttclub.backend.booking.dto;

/** Payload for creating a membership entitlement under a plan. */
public class EntitlementCreateReq {
    /** TABLE_HOURS | PROGRAM_CREDITS | TOURNAMENT_ENTRIES */
    public String kind;
    public Integer amount;
}
