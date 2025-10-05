package com.ttclub.backend.booking.dto;

import java.time.Instant;

public class ProgramOccurrenceDto {
    public Long   id;         // primary key
    public Long   programId;
    public String title;
    public Instant start;
    public Instant end;
    public String coachName;
}
