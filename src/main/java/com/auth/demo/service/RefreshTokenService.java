package com.auth.demo.service;

import com.auth.demo.exception.TokenRefreshException;
import com.auth.demo.model.RefreshToken;
import com.auth.demo.model.User;
import com.auth.demo.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for creating, validating, and rotating refresh tokens.
 */
@Service
public class RefreshTokenService {

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates and persists a new refresh token for the given user.
     */
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiryMs));
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Looks up a refresh token by its string value.
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verifies that the given refresh token has not expired.
     *
     * @throws TokenRefreshException if the token is expired (it is also deleted from the DB)
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token has expired. Please login again.");
        }
        return token;
    }

    /**
     * Deletes a single refresh token (used during token rotation and logout).
     */
    @Transactional
    public void deleteToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }

    /**
     * Deletes all refresh tokens for a user (used when changing password — forces re-login on all devices).
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
