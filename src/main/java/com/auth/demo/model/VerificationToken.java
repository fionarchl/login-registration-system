package com.auth.demo.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity for one-time email verification tokens.
 *
 * <p>Created at registration time and deleted once the user clicks the link
 * or when the token expires.</p>
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /* ── Constructors ── */

    public VerificationToken() {
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
