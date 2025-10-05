package com.ttclub.backend.booking.dto;

public class ProgramPackageDto {
    public Long id;
    public Long programId;
    public String name;            // example: "6 weeks"
    public Integer sessionsCount;
    public Double priceCad;
    public boolean active;
    public Integer sortOrder;      // optional UI sorting
}
