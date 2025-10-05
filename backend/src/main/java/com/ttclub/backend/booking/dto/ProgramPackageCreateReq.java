package com.ttclub.backend.booking.dto;

/** Payload for creating a package under a program. */
public class ProgramPackageCreateReq {
    public String name;
    public Integer sessionsCount;
    public Double priceCad;
    public Boolean active;
    public Integer sortOrder;
}
