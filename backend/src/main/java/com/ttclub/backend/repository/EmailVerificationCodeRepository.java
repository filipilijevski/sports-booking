package com.ttclub.backend.repository;

import com.ttclub.backend.model.EmailVerificationCode;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationCodeRepository
        extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findByEmail(String email);

    /** hard-delete the previous row so we can re-insert */
    @Modifying  //  make Spring run the query
    @Query("delete from EmailVerificationCode e where e.email = :email")
    void deleteByEmail(@Param("email") String email);

    Optional<EmailVerificationCode> findByEmailIgnoreCase(String email);

    void deleteByEmailIgnoreCase(String email);
}
