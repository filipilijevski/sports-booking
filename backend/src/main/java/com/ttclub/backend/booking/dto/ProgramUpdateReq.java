package com.ttclub.backend.booking.dto;

/** Payload for updating a program. */
public class ProgramUpdateReq {
    public String title;
    public String description;
    public Boolean active;
    /** Optional: "OPEN" | "ADMIN_ONLY". PUBLIC is accepted and mapped to OPEN. */
    public String enrollmentMode;
}
