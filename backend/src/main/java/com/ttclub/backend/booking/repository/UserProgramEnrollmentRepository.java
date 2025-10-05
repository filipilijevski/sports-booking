package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.UserProgramEnrollment;
import com.ttclub.backend.booking.model.UserProgramEnrollment.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProgramEnrollmentRepository extends JpaRepository<UserProgramEnrollment, Long> {

    boolean existsByUser_IdAndProgram_IdAndStatus(Long userId, Long programId, Status status);

    long countByUser_IdAndStatus(Long userId, Status status);

    @EntityGraph(attributePaths = {"user", "program", "programPackage"})
    @Query("select e from UserProgramEnrollment e")
    Page<UserProgramEnrollment> findAllWithGraph(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "program", "programPackage"})
    @Query("select e from UserProgramEnrollment e where e.status = :status")
    Page<UserProgramEnrollment> findByStatusWithGraph(@Param("status") Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "program", "programPackage"})
    @Query("""
           select e
             from UserProgramEnrollment e
             where lower(e.user.email) like concat('%', ?1, '%')
                or lower(e.user.firstName) like concat('%', ?1, '%')
                or lower(e.user.lastName)  like concat('%', ?1, '%')
                or lower(e.program.title)  like concat('%', ?1, '%')
                or lower(e.programPackage.name) like concat('%', ?1, '%')
           """)
    Page<UserProgramEnrollment> searchByUserProgramPackage(String q, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "program", "programPackage"})
    List<UserProgramEnrollment> findByUser_EmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"user", "program", "programPackage"})
    List<UserProgramEnrollment> findByUser_IdOrderByCreatedAtDesc(Long userId);

    boolean existsByUser_EmailAndProgram_IdAndStatus(String email, Long programId, Status status);

    /* attendance helpers */

    @EntityGraph(attributePaths = {"user", "program", "programPackage"})
    @Query("""
           select e
             from UserProgramEnrollment e
             join e.user u
            where e.program.id = :programId
              and e.status = :status
              and e.sessionsRemaining > 0
            order by u.firstName asc, u.lastName asc, u.email asc
           """)
    List<UserProgramEnrollment> findEligibleForProgram(@Param("programId") Long programId,
                                                       @Param("status") Status status);

    Optional<UserProgramEnrollment> findFirstByUser_IdAndProgram_IdAndStatus(Long userId, Long programId, Status status);
}
