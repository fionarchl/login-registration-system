package com.auth.demo.controller;

import com.auth.demo.dto.*;
import com.auth.demo.exception.RateLimitExceededException;
import com.auth.demo.service.AuthService;
import com.auth.demo.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing authentication endpoints.
 *
 * <h3>Public endpoints (no JWT required)</h3>
 * <ul>
 *   <li>{@code POST /register}   — Register a new account</li>
 *   <li>{@code GET  /verify}     — Verify email via token</li>
 *   <li>{@code POST /login}      — Login → JWT access + refresh tokens</li>
 *   <li>{@code POST /refresh-token} — Refresh access token</li>
 *   <li>{@code POST /logout}     — Invalidate refresh token</li>
 * </ul>
 *
 * <h3>Protected endpoints (JWT required)</h3>
 * <ul>
 *   <li>{@code POST /change-password} — Change the authenticated user's password</li>
 * </ul>
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;

    public AuthController(AuthService authService, RateLimitingService rateLimitingService) {
        this.authService = authService;
        this.rateLimitingService = rateLimitingService;
    }

    /* ── Registration ── */

    /**
     * Registers a new user account and sends a verification email.
     *
     * @return 201 Created on success
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody AuthRequest request) {
        ApiResponse response = authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ── Email Verification ── */

    /**
     * Verifies the user's email address via a one-time token.
     *
     * @param token the verification token from the email link
     * @return 200 OK on success
     */
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam String token) {
        ApiResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    /* ── Login ── */

    /**
     * Authenticates a user and returns JWT access + refresh tokens.
     * Rate-limited to {@code app.rate-limit.login.max-attempts} requests
     * per {@code app.rate-limit.login.window-seconds} seconds per IP.
     *
     * @return 200 OK with tokens on success
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                              HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        if (rateLimitingService.isRateLimited(ipAddress)) {
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again later.");
        }

        AuthResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    /* ── Token Refresh ── */

    /**
     * Issues a new access token (and rotated refresh token) using a valid refresh token.
     *
     * @return 200 OK with new tokens
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /* ── Change Password (authenticated) ── */

    /**
     * Changes the authenticated user's password. Requires a valid JWT in the
     * {@code Authorization: Bearer <token>} header.
     *
     * <p>All existing refresh tokens are invalidated (forces re-login on all devices).</p>
     *
     * @return 200 OK on success
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                      Authentication authentication) {
        String email = authentication.getName();
        ApiResponse response = authService.changePassword(
                email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(response);
    }

    /* ── Logout ── */

    /**
     * Invalidates the given refresh token.
     *
     * @return 200 OK on success
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        ApiResponse response = authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}
