package com.ttclub.backend.repository;

import com.ttclub.backend.model.PreAuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PreAuthSessionRepository extends JpaRepository<PreAuthSession, Long> {
    Optional<PreAuthSession> findByTokenHash(String tokenHash);
    long deleteByExpiresAtBefore(Instant t);
}
