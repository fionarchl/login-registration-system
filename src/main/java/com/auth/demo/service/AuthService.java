package com.auth.demo.service;

import com.auth.demo.dto.ApiResponse;
import com.auth.demo.dto.AuthResponse;

/**
 * Contract for authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user account and sends a verification email.
     */
    ApiResponse register(String email, String password);

    /**
     * Authenticates a user and returns JWT access + refresh tokens.
     */
    AuthResponse login(String email, String password);

    /**
     * Verifies a user's email address via a one-time token.
     */
    ApiResponse verifyEmail(String token);

    /**
     * Issues a new access token (and rotated refresh token) using a valid refresh token.
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Changes the authenticated user's password and invalidates all refresh tokens.
     */
    ApiResponse changePassword(String userEmail, String currentPassword, String newPassword);

    /**
     * Invalidates a refresh token (logout).
     */
    ApiResponse logout(String refreshToken);
}
