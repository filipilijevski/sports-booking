package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.ProgramOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProgramOccurrenceRepository extends JpaRepository<ProgramOccurrence, Long> {

    boolean existsByProgram_IdAndStartTs(Long programId, Instant startTs);

    /* Public/admin feeds exclude cancelled occurrences */
    @Query("""
           select o
             from ProgramOccurrence o
             join fetch o.program p
             left join fetch o.coach c
            where o.cancelled = false
              and o.startTs >= :from and o.startTs < :to
            order by o.startTs asc
           """)
    List<ProgramOccurrence> findAllInRangeFetch(@Param("from") Instant from,
                                                @Param("to") Instant to);

    @Query("""
           select o
             from ProgramOccurrence o
             join fetch o.program p
             left join fetch o.coach c
            where o.cancelled = false
              and p.id = :programId
              and o.startTs >= :from and o.startTs < :to
            order by o.startTs asc
           """)
    List<ProgramOccurrence> findByProgramInRangeFetch(@Param("programId") Long programId,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to);

    /* Keys for idempotence (includes cancelled=true) */
    @Query("""
           select o
             from ProgramOccurrence o
            where o.startTs >= :from and o.startTs < :to
           """)
    List<ProgramOccurrence> findKeysInRange(@Param("from") Instant from,
                                            @Param("to") Instant to);

    /* Program-specific keys (includes cancelled=true) */
    @Query("""
           select o
             from ProgramOccurrence o
            where o.program.id = :programId
              and o.startTs >= :from and o.startTs < :to
           """)
    List<ProgramOccurrence> findAllForProgramInRange(@Param("programId") Long programId,
                                                     @Param("from") Instant from,
                                                     @Param("to") Instant to);

    /* For cancel or rebuild */
    @Query("""
           select o
             from ProgramOccurrence o
            where o.program.id = :programId
              and o.cancelled = false
              and o.startTs >= :from
            order by o.startTs asc
           """)
    List<ProgramOccurrence> findActiveFutureForProgram(@Param("programId") Long programId,
                                                       @Param("from") Instant from);
}
