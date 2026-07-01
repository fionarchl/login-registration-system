package com.auth.demo.service.impl;

import com.auth.demo.dto.ApiResponse;
import com.auth.demo.dto.AuthResponse;
import com.auth.demo.exception.*;
import com.auth.demo.model.RefreshToken;
import com.auth.demo.model.User;
import com.auth.demo.model.VerificationToken;
import com.auth.demo.repository.UserRepository;
import com.auth.demo.repository.VerificationTokenRepository;
import com.auth.demo.service.EmailService;
import com.auth.demo.service.JwtService;
import com.auth.demo.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthServiceImpl}.
 * Uses Mockito to isolate the service from JPA repositories and external services.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailService emailService;

    @InjectMocks private AuthServiceImpl authService;

    private final PasswordEncoder realEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        // Use a real BCrypt encoder wrapped in a spy so Mockito injects it
        // but actual encoding/matching works correctly
        ReflectionTestUtils.setField(authService, "passwordEncoder", realEncoder);
        ReflectionTestUtils.setField(authService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutDurationMinutes", 15);
        ReflectionTestUtils.setField(authService, "verificationTokenExpiryHours", 24);
    }

    /* ═══════════════════════════════════════════
     *  Registration Tests
     * ═══════════════════════════════════════════ */

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("registers a new user successfully")
        void shouldRegisterSuccessfully() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse response = authService.register("test@example.com", "MyPass1@rd");

            assertTrue(response.isSuccess());
            assertNotNull(response.getMessage());
            verify(userRepository).save(any(User.class));
            verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("rejects duplicate email")
        void shouldRejectDuplicateEmail() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThrows(DuplicateEmailException.class,
                    () -> authService.register("test@example.com", "MyPass1@rd"));
        }

        @Test
        @DisplayName("rejects email missing '@'")
        void shouldRejectEmailWithoutAt() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("testexample.com", "MyPass1@rd"));
        }

        @Test
        @DisplayName("rejects email missing '.'")
        void shouldRejectEmailWithoutDot() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("test@example", "MyPass1@rd"));
        }

        @Test
        @DisplayName("sanitizes email (trim + lowercase)")
        void shouldSanitizeEmail() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.register("  Test@Example.COM  ", "MyPass1@rd");

            verify(userRepository).existsByEmail("test@example.com");
        }
    }

    /* ═══════════════════════════════════════════
     *  Email Verification Tests
     * ═══════════════════════════════════════════ */

    @Nested
    @DisplayName("Email Verification")
    class VerificationTests {

        @Test
        @DisplayName("verifies email with valid token")
        void shouldVerifyEmail() {
            User user = new User("test@example.com", "encoded");
            VerificationToken vt = new VerificationToken();
            vt.setToken("valid-token");
            vt.setUser(user);
            vt.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));

            when(verificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(vt));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse response = authService.verifyEmail("valid-token");

            assertTrue(response.isSuccess());
            assertTrue(user.isEmailVerified());
            verify(verificationTokenRepository).delete(vt);
        }

        @Test
        @DisplayName("rejects expired verification token")
        void shouldRejectExpiredToken() {
            VerificationToken vt = new VerificationToken();
            vt.setToken("expired-token");
            vt.setUser(new User("test@example.com", "encoded"));
            vt.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));

            when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(vt));

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.verifyEmail("expired-token"));
        }

        @Test
        @DisplayName("rejects invalid verification token")
        void shouldRejectInvalidToken() {
            when(verificationTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.verifyEmail("invalid"));
        }
    }

    /* ═══════════════════════════════════════════
     *  Login Tests
     * ═══════════════════════════════════════════ */

    @Nested
    @DisplayName("Login")
    class LoginTests {

        private User verifiedUser;

        @BeforeEach
        void setUpUser() {
            verifiedUser = new User("test@example.com", realEncoder.encode("MyPass1@rd"));
            verifiedUser.setEmailVerified(true);
        }

        @Test
        @DisplayName("succeeds with valid credentials")
        void shouldLoginSuccessfully() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));
            when(jwtService.generateAccessToken("test@example.com")).thenReturn("jwt-token");
            when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);

            RefreshToken rt = new RefreshToken();
            rt.setToken("refresh-token");
            when(refreshTokenService.createRefreshToken(verifiedUser)).thenReturn(rt);

            AuthResponse response = authService.login("test@example.com", "MyPass1@rd");

            assertEquals("jwt-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals(900, response.getExpiresIn());
        }

        @Test
        @DisplayName("rejects wrong password and increments failure count")
        void shouldRejectWrongPassword() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login("test@example.com", "WrongPass1@"));

            assertEquals(1, verifiedUser.getFailedLoginAttempts());
            verify(userRepository).save(verifiedUser);
        }

        @Test
        @DisplayName("rejects unverified email")
        void shouldRejectUnverifiedEmail() {
            verifiedUser.setEmailVerified(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));

            assertThrows(EmailNotVerifiedException.class,
                    () -> authService.login("test@example.com", "MyPass1@rd"));
        }

        @Test
        @DisplayName("locks account after max failed attempts")
        void shouldLockAccountAfterMaxAttempts() {
            verifiedUser.setFailedLoginAttempts(4); // one more triggers lock
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login("test@example.com", "WrongPass1@"));

            assertEquals(5, verifiedUser.getFailedLoginAttempts());
            assertNotNull(verifiedUser.getLockTime());
        }

        @Test
        @DisplayName("rejects login on locked account")
        void shouldRejectLockedAccount() {
            verifiedUser.setFailedLoginAttempts(5);
            verifiedUser.setLockTime(java.time.LocalDateTime.now());
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));

            assertThrows(AccountLockedException.class,
                    () -> authService.login("test@example.com", "MyPass1@rd"));
        }

        @Test
        @DisplayName("resets failure count on successful login")
        void shouldResetFailureCountOnSuccess() {
            verifiedUser.setFailedLoginAttempts(3);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));
            when(jwtService.generateAccessToken(anyString())).thenReturn("jwt");
            when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);
            when(refreshTokenService.createRefreshToken(any())).thenReturn(createRefreshToken());

            authService.login("test@example.com", "MyPass1@rd");

            assertEquals(0, verifiedUser.getFailedLoginAttempts());
            assertNull(verifiedUser.getLockTime());
        }
    }

    /* ═══════════════════════════════════════════
     *  Change Password Tests
     * ═══════════════════════════════════════════ */

    @Nested
    @DisplayName("Change Password")
    class ChangePasswordTests {

        @Test
        @DisplayName("changes password successfully")
        void shouldChangePassword() {
            User user = new User("test@example.com", realEncoder.encode("OldPass1@"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse response = authService.changePassword("test@example.com", "OldPass1@", "NewPass1@");

            assertTrue(response.isSuccess());
            assertTrue(realEncoder.matches("NewPass1@", user.getPassword()));
            verify(refreshTokenService).deleteByUser(user);
        }

        @Test
        @DisplayName("rejects wrong current password")
        void shouldRejectWrongCurrentPassword() {
            User user = new User("test@example.com", realEncoder.encode("OldPass1@"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.changePassword("test@example.com", "WrongPass1@", "NewPass1@"));
        }
    }

    /* ═══════════════════════════════════════════
     *  Refresh Token Tests
     * ═══════════════════════════════════════════ */

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshes token successfully with rotation")
        void shouldRefreshToken() {
            User user = new User("test@example.com", "encoded");
            RefreshToken oldToken = new RefreshToken();
            oldToken.setToken("old-refresh");
            oldToken.setUser(user);
            oldToken.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));

            RefreshToken newToken = new RefreshToken();
            newToken.setToken("new-refresh");

            when(refreshTokenService.findByToken("old-refresh")).thenReturn(Optional.of(oldToken));
            when(refreshTokenService.verifyExpiration(oldToken)).thenReturn(oldToken);
            when(refreshTokenService.createRefreshToken(user)).thenReturn(newToken);
            when(jwtService.generateAccessToken("test@example.com")).thenReturn("new-jwt");
            when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);

            AuthResponse response = authService.refreshToken("old-refresh");

            assertEquals("new-jwt", response.getAccessToken());
            assertEquals("new-refresh", response.getRefreshToken());
            verify(refreshTokenService).deleteToken(oldToken);
        }

        @Test
        @DisplayName("rejects invalid refresh token")
        void shouldRejectInvalidRefreshToken() {
            when(refreshTokenService.findByToken("invalid")).thenReturn(Optional.empty());

            assertThrows(TokenRefreshException.class,
                    () -> authService.refreshToken("invalid"));
        }
    }

    /* ═══════════════════════════════════════════
     *  Logout Tests
     * ═══════════════════════════════════════════ */

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("logs out successfully")
        void shouldLogout() {
            RefreshToken token = new RefreshToken();
            token.setToken("refresh-token");
            when(refreshTokenService.findByToken("refresh-token")).thenReturn(Optional.of(token));

            ApiResponse response = authService.logout("refresh-token");

            assertTrue(response.isSuccess());
            verify(refreshTokenService).deleteToken(token);
        }
    }

    /* ── Helper ── */

    private RefreshToken createRefreshToken() {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        return rt;
    }
}
