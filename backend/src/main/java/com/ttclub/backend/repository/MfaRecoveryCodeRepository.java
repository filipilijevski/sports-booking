package com.ttclub.backend.repository;

import com.ttclub.backend.model.MfaRecoveryCode;
import com.ttclub.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, Long> {
    Optional<MfaRecoveryCode> findByUserAndCodeHash(User user, String codeHash);
    List<MfaRecoveryCode> findByUser(User user);
    void deleteByUser(User user);
}
