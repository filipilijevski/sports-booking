package com.ttclub.backend.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stores a SHA-256 hex digest of the refresh token.
     * Column name is "token_hash" for clarity in the DB.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String token;

    /** Owning user */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected RefreshToken() { }

    public RefreshToken(String token, User user, Instant expiresAt) {
        this.token     = token;
        this.user      = user;
        this.expiresAt = expiresAt;
    }

    public Long      getId()        { return id; }
    public String    getToken()     { return token; }
    public User      getUser()      { return user; }
    public Instant   getExpiresAt() { return expiresAt; }
    public Instant   getCreatedAt() { return createdAt; }

    public void setId(Long id)                { this.id = id; }
    public void setToken(String token)        { this.token = token; }
    public void setUser(User user)            { this.user = user; }
    public void setExpiresAt(Instant expires) { this.expiresAt = expires; }
    public void setCreatedAt(Instant created) { this.createdAt = created; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        return Objects.equals(token, that.token);
    }
    @Override public int hashCode() { return Objects.hash(token); }

    @Override public String toString() {
        return "RefreshToken{token='%s', userId=%d}".formatted(token,
                user != null ? user.getId() : null);
    }
}
