package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    /** LOCAL - e-mail + password, GOOGLE - OAuth2 login */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    private String firstName;
    private String lastName;
    private Instant createdAt = Instant.now();

    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret_enc")
    private String mfaSecretEnc;

    @Column(name = "mfa_secret_tmp_enc")
    private String mfaSecretTmpEnc;

    /** Temporary password hash (BCrypt) - separate from the real password hash */
    @Column(name = "temp_password_hash", length = 255)
    private String tempPasswordHash;

    /** Expiry for the temporary password */
    @Column(name = "temp_password_expires_at")
    private Instant tempPasswordExpiresAt;

    /** Soft delete timestamp */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public User() { }

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }
    public String getEmail()               { return email; }
    public void setEmail(String email)     { this.email = email; }
    public String getPasswordHash()        { return passwordHash; }
    public void setPasswordHash(String pw) { this.passwordHash = pw; }

    public AuthProvider getProvider()           { return provider; }
    public void setProvider(AuthProvider p)     { this.provider = p; }

    public Role getRole()                  { return role; }
    public void setRole(Role role)         { this.role = role; }
    public String getFirstName()           { return firstName; }
    public void setFirstName(String fn)    { this.firstName = fn; }
    public String getLastName()            { return lastName; }
    public void setLastName(String ln)     { this.lastName = ln; }
    public Instant getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Instant t)    { this.createdAt = t; }

    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean enabled) { this.mfaEnabled = enabled; }
    public String getMfaSecretEnc() { return mfaSecretEnc; }
    public void setMfaSecretEnc(String enc) { this.mfaSecretEnc = enc; }
    public String getMfaSecretTmpEnc() { return mfaSecretTmpEnc; }
    public void setMfaSecretTmpEnc(String enc) { this.mfaSecretTmpEnc = enc; }

    public String getTempPasswordHash() { return tempPasswordHash; }
    public void setTempPasswordHash(String tempPasswordHash) { this.tempPasswordHash = tempPasswordHash; }
    public Instant getTempPasswordExpiresAt() { return tempPasswordExpiresAt; }
    public void setTempPasswordExpiresAt(Instant tempPasswordExpiresAt) { this.tempPasswordExpiresAt = tempPasswordExpiresAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public String toString() { return this.firstName + " " + this.lastName;}
}
