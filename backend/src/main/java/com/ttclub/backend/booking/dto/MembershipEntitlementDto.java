package com.ttclub.backend.booking.dto;

public class MembershipEntitlementDto {
    public Long id;
    public Long planId;
    public String kind;   // TABLE_HOURS | PROGRAM_CREDITS | TOURNAMENT_ENTRIES
    public Double amount; // allows fractional hours/entries if needed
}
