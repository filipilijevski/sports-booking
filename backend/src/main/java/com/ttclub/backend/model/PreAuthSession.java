package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pre_auth_sessions")
public class PreAuthSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 64-char SHA-256 hex of the raw token */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreAuthPurpose purpose;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt = Instant.now();

    /** Propagate "must change password" across MFA verification when temp password is used */
    @Column(name = "pwd_change_required", nullable = false)
    private boolean pwdChangeRequired = false;

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public PreAuthPurpose getPurpose() { return purpose; }
    public void setPurpose(PreAuthPurpose purpose) { this.purpose = purpose; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isPwdChangeRequired() { return pwdChangeRequired; }
    public void setPwdChangeRequired(boolean pwdChangeRequired) { this.pwdChangeRequired = pwdChangeRequired; }
}
