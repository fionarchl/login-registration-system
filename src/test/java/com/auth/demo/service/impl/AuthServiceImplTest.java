package com.auth.demo.service.impl;

import com.auth.demo.dto.ApiResponse;
import com.auth.demo.exception.DuplicateEmailException;
import com.auth.demo.exception.InvalidCredentialsException;
import com.auth.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AuthServiceImpl}.
 * Uses real BCrypt encoder and in-memory repository (no mocking needed).
 */
class AuthServiceImplTest {

    private AuthServiceImpl authService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(userRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("registers a new user with valid credentials")
        void shouldRegisterSuccessfully() {
            ApiResponse response = authService.register("test@example.com", "mypassword");

            assertTrue(response.isSuccess());
            assertEquals("User registered successfully.", response.getMessage());
            assertTrue(userRepository.existsByEmail("test@example.com"));
        }

        @Test
        @DisplayName("rejects duplicate email")
        void shouldRejectDuplicateEmail() {
            authService.register("test@example.com", "mypassword");

            DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                    () -> authService.register("test@example.com", "anotherpassword"));
            assertEquals("An account with this email already exists.", ex.getMessage());
        }

        @Test
        @DisplayName("treats email as case-insensitive")
        void shouldHandleCaseInsensitiveEmail() {
            authService.register("Test@Example.COM", "mypassword");

            assertThrows(DuplicateEmailException.class,
                    () -> authService.register("test@example.com", "anotherpassword"));
        }

        @Test
        @DisplayName("stores password as BCrypt hash, not plaintext")
        void shouldEncryptPassword() {
            authService.register("test@example.com", "mypassword");

            var user = userRepository.findByEmail("test@example.com").orElseThrow();
            assertNotEquals("mypassword", user.getPassword());
            assertTrue(user.getPassword().startsWith("$2a$"));
        }

        @Test
        @DisplayName("trims and lowercases email before storage")
        void shouldSanitizeEmail() {
            authService.register("  User@Example.COM  ", "mypassword");

            assertTrue(userRepository.existsByEmail("user@example.com"));
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @BeforeEach
        void registerTestUser() {
            authService.register("test@example.com", "mypassword");
        }

        @Test
        @DisplayName("succeeds with correct credentials")
        void shouldLoginSuccessfully() {
            ApiResponse response = authService.login("test@example.com", "mypassword");

            assertTrue(response.isSuccess());
            assertEquals("Login successful.", response.getMessage());
        }

        @Test
        @DisplayName("rejects wrong password")
        void shouldRejectWrongPassword() {
            InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                    () -> authService.login("test@example.com", "wrongpassword"));
            assertEquals("Invalid email or password.", ex.getMessage());
        }

        @Test
        @DisplayName("rejects non-existent email")
        void shouldRejectNonExistentEmail() {
            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login("nobody@example.com", "mypassword"));
        }

        @Test
        @DisplayName("handles case-insensitive email during login")
        void shouldHandleCaseInsensitiveEmailLogin() {
            ApiResponse response = authService.login("TEST@EXAMPLE.COM", "mypassword");
            assertTrue(response.isSuccess());
        }
    }
}
