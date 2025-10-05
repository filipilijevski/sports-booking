package com.ttclub.backend.booking.dto;

/** Simple HH:mm to HH:mm range with optional coach display name */
public class TimeRange {
    /** "HH:mm" */
    public String start;
    /** "HH:mm" */
    public String end;
    /** Optional coach name */
    public String coach;
}
