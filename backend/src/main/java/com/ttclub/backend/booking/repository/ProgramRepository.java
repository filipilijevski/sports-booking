package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findAllByActiveTrueOrderByTitleAsc();
}
