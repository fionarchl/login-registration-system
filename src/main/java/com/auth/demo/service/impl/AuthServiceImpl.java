package com.auth.demo.service.impl;

import com.auth.demo.dto.ApiResponse;
import com.auth.demo.dto.AuthResponse;
import com.auth.demo.exception.*;
import com.auth.demo.model.RefreshToken;
import com.auth.demo.model.User;
import com.auth.demo.model.VerificationToken;
import com.auth.demo.repository.UserRepository;
import com.auth.demo.repository.VerificationTokenRepository;
import com.auth.demo.service.*;
import com.auth.demo.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Core authentication logic.
 *
 * <ul>
 *   <li>Emails are sanitized and validated (must contain {@code @} and {@code .}).</li>
 *   <li>Passwords are hashed with BCrypt before storage.</li>
 *   <li>Login returns JWT access + refresh tokens.</li>
 *   <li>Refresh tokens are rotated on each use (old token invalidated, new one issued).</li>
 *   <li>Account locks after N consecutive failed login attempts (auto-unlocks after configurable duration).</li>
 *   <li>Email must be verified before login is allowed.</li>
 *   <li>Login failure messages are intentionally vague to prevent user-enumeration attacks.</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Value("${app.lockout.max-attempts}")
    private int maxLoginAttempts;

    @Value("${app.lockout.duration-minutes}")
    private int lockoutDurationMinutes;

    @Value("${app.verification.token-expiry-hours}")
    private int verificationTokenExpiryHours;

    public AuthServiceImpl(UserRepository userRepository,
                           VerificationTokenRepository verificationTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }

    /* ═══════════════════════════════════════════════════════════
     *  REGISTER
     * ═══════════════════════════════════════════════════════════ */

    @Override
    @Transactional
    public ApiResponse register(String email, String password) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(email);
        InputSanitizer.validateEmailFormat(sanitizedEmail);

        if (userRepository.existsByEmail(sanitizedEmail)) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        // Create user (unverified)
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(sanitizedEmail, encodedPassword);
        userRepository.save(user);

        // Create verification token
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(Instant.now().plus(verificationTokenExpiryHours, ChronoUnit.HOURS));
        verificationTokenRepository.save(verificationToken);

        // Send verification email (logged to console in dev mode)
        emailService.sendVerificationEmail(sanitizedEmail, verificationToken.getToken());

        log.info("New user registered: {}", sanitizedEmail);
        return ApiResponse.success("User registered successfully. Please check your email to verify your account.");
    }

    /* ═══════════════════════════════════════════════════════════
     *  VERIFY EMAIL
     * ═══════════════════════════════════════════════════════════ */

    @Override
    @Transactional
    public ApiResponse verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid verification token."));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new InvalidCredentialsException("Verification token has expired. Please register again.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
        return ApiResponse.success("Email verified successfully. You can now login.");
    }

    /* ═══════════════════════════════════════════════════════════
     *  LOGIN
     * ═══════════════════════════════════════════════════════════ */

    @Override
    @Transactional
    public AuthResponse login(String email, String password) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(email);
        InputSanitizer.validateEmailFormat(sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login attempt with unregistered email: {}", sanitizedEmail);
                    return new InvalidCredentialsException("Invalid email or password.");
                });

        // Check account lockout
        checkAccountLock(user);

        // Check email verification
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                    "Email address not verified. Please check your email for the verification link.");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            handleFailedLogin(user);
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        // Reset failed attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(sanitizedEmail);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User logged in: {}", sanitizedEmail);
        return new AuthResponse(accessToken, refreshToken.getToken(),
                jwtService.getAccessTokenExpiryMs() / 1000);
    }

    /* ═══════════════════════════════════════════════════════════
     *  REFRESH TOKEN
     * ═══════════════════════════════════════════════════════════ */

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token."));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Rotate: delete old refresh token, issue new one
        refreshTokenService.deleteToken(refreshToken);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        String accessToken = jwtService.generateAccessToken(user.getEmail());

        log.info("Token refreshed for user: {}", user.getEmail());
        return new AuthResponse(accessToken, newRefreshToken.getToken(),
                jwtService.getAccessTokenExpiryMs() / 1000);
    }

    /* ═══════════════════════════════════════════════════════════
     *  CHANGE PASSWORD
     * ═══════════════════════════════════════════════════════════ */

    @Override
    @Transactional
    public ApiResponse changePassword(String userEmail, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new InvalidCredentialsException("User not found."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all refresh tokens → force re-login on all devices
        refreshTokenService.deleteByUser(user);

        log.info("Password changed for user: {}", userEmail);
        return ApiResponse.success("Password changed successfully. Please login again with your new password.");
    }

    /* ═══════════════════════════════════════════════════════════
     *  LOGOUT
     * ═══════════════════════════════════════════════════════════ */

    @Override
    @Transactional
    public ApiResponse logout(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token."));

        refreshTokenService.deleteToken(refreshToken);

        log.info("User logged out (refresh token invalidated)");
        return ApiResponse.success("Logged out successfully.");
    }

    /* ═══════════════════════════════════════════════════════════
     *  INTERNAL HELPERS
     * ═══════════════════════════════════════════════════════════ */

    /**
     * Checks whether the account is currently locked. If the lock has expired,
     * the lockout state is reset automatically.
     */
    private void checkAccountLock(User user) {
        if (user.getLockTime() == null) {
            return;
        }

        LocalDateTime unlockTime = user.getLockTime().plusMinutes(lockoutDurationMinutes);

        if (LocalDateTime.now().isBefore(unlockTime)) {
            long minutesRemaining = Duration.between(LocalDateTime.now(), unlockTime).toMinutes() + 1;
            throw new AccountLockedException(
                    "Account is locked due to too many failed login attempts. Try again in "
                            + minutesRemaining + " minute(s).");
        }

        // Lock has expired — reset
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
    }

    /**
     * Increments the failed login counter and locks the account if the threshold is reached.
     */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxLoginAttempts) {
            user.setLockTime(LocalDateTime.now());
            log.warn("Account locked for email: {} after {} failed attempts", user.getEmail(), attempts);
        }

        userRepository.save(user);
    }
}
