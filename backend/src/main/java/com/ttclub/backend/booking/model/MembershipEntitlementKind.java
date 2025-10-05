package com.ttclub.backend.booking.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kind of benefit granted by a membership plan.<br>
 * Values are aligned with DB check constraints:
 *   TABLE_HOURS | PROGRAM_CREDITS | TOURNAMENT_ENTRIES
 */
public enum MembershipEntitlementKind {
    TABLE_HOURS,
    PROGRAM_CREDITS,
    TOURNAMENT_ENTRIES;

    /** Serialize as the enum name (i.e "TABLE_HOURS") */
    @JsonValue
    public String toJson() {
        return name();
    }

    /**
     * Parser for JSON or other inputs. Accepts case-insensitive and
     * hyphen/space variants (e.g., "table-hours", "Table Hours").
     */
    @JsonCreator
    public static MembershipEntitlementKind fromJson(String value) {
        if (value == null) return null;
        String norm = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return MembershipEntitlementKind.valueOf(norm);
    }
}
