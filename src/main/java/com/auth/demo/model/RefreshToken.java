package com.auth.demo.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity for long-lived refresh tokens.
 *
 * <p>Each token is tied to a single {@link User}. A user may hold multiple
 * refresh tokens (e.g. one per device). Tokens are rotated on each use and
 * deleted on logout or password change.</p>
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /* ── Constructors ── */

    public RefreshToken() {
    }

    /* ── Helpers ── */

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /* ── Getters & Setters ── */

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }
}
