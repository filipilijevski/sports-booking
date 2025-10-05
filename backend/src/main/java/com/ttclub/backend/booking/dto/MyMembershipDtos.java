package com.ttclub.backend.booking.dto;

import java.time.Instant;
import java.util.List;

public class MyMembershipDtos {

    public static class MyMembership {
        public Long userMembershipId;
        public Long planId;
        public String planName;
        public String planType;       // INITIAL | SPECIAL
        public String holderKind;     // INDIVIDUAL | GROUP
        public Long groupId;          // null for INDIVIDUAL
        public boolean isGroupOwner;  // true if group and you're the owner
        public Instant startTs;
        public Instant endTs;
        public boolean active;
        public Entitlements entitlements;
        public boolean canRenew;
        public long daysUntilExpiry;
    }

    public static class Entitlements {
        public Double tableHoursRemaining;        // may be null
        public Double programCreditsRemaining;    // may be null
        public Double tournamentEntriesRemaining; // may be null
    }

    public static class MyMembershipList {
        public List<MyMembership> items;
    }
}
