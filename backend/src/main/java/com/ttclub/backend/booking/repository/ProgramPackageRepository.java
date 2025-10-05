package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.ProgramPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProgramPackageRepository extends JpaRepository<ProgramPackage, Long> {

    // basic derived query
    List<ProgramPackage> findByProgramIdOrderBySortOrderAscIdAsc(Long programId);

    long countByProgramIdAndActiveTrue(Long programId);

    // bulk load for multiple programs (try to avoid N+1)
    List<ProgramPackage> findByProgram_IdInOrderBySortOrderAscIdAsc(Collection<Long> programIds);

    // For NULLS LAST consistently, uncomment below and use this instead:
    // @Query("select pp from ProgramPackage pp where pp.program.id = :programId order by " +
    //        "case when pp.sortOrder is null then 1 else 0 end, pp.sortOrder asc, pp.id asc")
    // List<ProgramPackage> findOrderedByProgramIdNullsLast(Long programId);
}
