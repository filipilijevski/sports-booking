package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.Enrollment;
import com.ttclub.backend.booking.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-model access for enrollments.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Counts enrollments for a user within a program filtered by status.
     */
    long countByUserIdAndProgramIdAndStatus(Long userId, Long programId, EnrollmentStatus status);
}
