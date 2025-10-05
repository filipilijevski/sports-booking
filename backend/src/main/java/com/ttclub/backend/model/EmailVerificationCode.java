package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * One temporary 6-digit verification code per e-mail address.
 * Rows are removed after successful registration or when expired.
 */
@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCode {

    /* Fields */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** E-mail this code belongs to (unique) */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** 6-digit numeric code as a String to keep leading zeros */
    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** JPA / Hibernate requirement */
    protected EmailVerificationCode() {}

    public EmailVerificationCode(String email, String code, Instant expiresAt) {
        this.email     = email;
        this.code      = code;
        this.expiresAt = expiresAt;
    }

    public Long getId()                 { return id;      }
    public String getEmail()            { return email;   }
    public String getCode()             { return code;    }
    public Instant getExpiresAt()       { return expiresAt; }

    public void setId(Long id)                  { this.id = id; }
    public void setEmail(String email)          { this.email = email; }
    public void setCode(String code)            { this.code = code; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailVerificationCode that)) return false;
        return Objects.equals(email, that.email);
    }
    @Override public int hashCode() { return Objects.hash(email); }

    @Override public String toString() {
        return "EmailVerificationCode{" +
                "email='" + email + '\'' +
                ", code='" + code + '\'' +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
