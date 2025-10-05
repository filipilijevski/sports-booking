package com.ttclub.backend.booking.service;

import com.ttclub.backend.booking.repository.UserMembershipRepository;
import org.springframework.stereotype.Component;

@Component
public class MembershipGuard {

    private final UserMembershipRepository userMemberships;

    public MembershipGuard(UserMembershipRepository userMemberships) {
        this.userMemberships = userMemberships;
    }

    /**
     * Throws IllegalStateException if the user lacks an active INITIAL membership.<br>
     * Business rule:<br>
     *   - Users can KEEP existing enrollments;<br>
     *   - BUT cannot enroll into new programs or ATTEND (decrement sessions)
     *     unless they have an active INITIAL membership.
     */
    public void ensureInitialMembershipActive(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        boolean ok = !userMemberships.findActiveInitialMembershipsForUser(userId).isEmpty();
        if (!ok) {
            throw new IllegalStateException(
                    "Initial annual club membership is not active. " +
                            "Please renew to enroll or attend sessions."
            );
        }
    }

    public boolean hasActiveInitialMembership(Long userId) {
        return !userMemberships.findActiveInitialMembershipsForUser(userId).isEmpty();
    }
}
