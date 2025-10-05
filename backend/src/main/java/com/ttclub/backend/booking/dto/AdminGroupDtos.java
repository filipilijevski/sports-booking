package com.ttclub.backend.booking.dto;

import java.time.Instant;
import java.util.List;

public class AdminGroupDtos {

    public static class GroupListItem {
        public Long id;
        public Long planId;
        public String planName;
        public String planType;       // INITIAL | SPECIAL
        public String holderKind;     // GROUP
        public Long ownerId;
        public String ownerName;
        public String ownerEmail;
        public Instant startTs;
        public Instant endTs;
        public boolean active;
        public long membersCount;
        public EntitlementsSummary entitlements; // remaining summary
    }

    public static class GroupDetail extends GroupListItem {
        public List<MemberRow> members;
    }

    public static class MemberRow {
        public Long userMembershipId;
        public Long userId;
        public String name;
        public String email;
        public boolean active;
    }

    public static class EntitlementsSummary {
        public Double tableHoursRemaining;      // null if not applicable
        public Double programCreditsRemaining;  // null if not applicable
        public Double tournamentEntriesRemaining; // null if not applicable
    }
}
