package com.auth.demo.controller;

import com.auth.demo.model.User;
import com.auth.demo.model.VerificationToken;
import com.auth.demo.repository.UserRepository;
import com.auth.demo.repository.VerificationTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests that boot the full Spring context with H2 and send real
 * HTTP requests through the authentication flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private VerificationTokenRepository verificationTokenRepository;

    private static final String TEST_EMAIL = "integration@test.com";
    private static final String TEST_PASSWORD = "SecurePass1@";

    /* ═══════════════════════════════════════════
     *  Registration
     * ═══════════════════════════════════════════ */

    @Test
    @Order(1)
    @DisplayName("POST /register → 201 Created")
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "User registered successfully. Please check your email to verify your account."));
    }

    @Test
    @Order(2)
    @DisplayName("POST /register duplicate → 409 Conflict")
    void shouldRejectDuplicateRegistration() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(3)
    @DisplayName("POST /register invalid email → 400 Bad Request")
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "%s"}
                                """.formatted(TEST_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(4)
    @DisplayName("POST /register weak password → 400 Bad Request")
    void shouldRejectWeakPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "weak@test.com", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(5)
    @DisplayName("POST /register password missing special char → 400")
    void shouldRejectPasswordWithoutSpecialChar() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nospecial@test.com", "password": "NoSpecialChar1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /* ═══════════════════════════════════════════
     *  Login before verification
     * ═══════════════════════════════════════════ */

    @Test
    @Order(6)
    @DisplayName("POST /login before email verification → 403 Forbidden")
    void shouldRejectLoginBeforeVerification() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    /* ═══════════════════════════════════════════
     *  Email Verification
     * ═══════════════════════════════════════════ */

    @Test
    @Order(7)
    @DisplayName("GET /verify → 200 OK")
    void shouldVerifyEmail() throws Exception {
        // Retrieve the verification token from the database
        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        VerificationToken vt = verificationTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/verify").param("token", vt.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully. You can now login."));
    }

    @Test
    @Order(8)
    @DisplayName("GET /verify invalid token → 401")
    void shouldRejectInvalidVerificationToken() throws Exception {
        mockMvc.perform(get("/verify").param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    /* ═══════════════════════════════════════════
     *  Login (after verification)
     * ═══════════════════════════════════════════ */

    @Test
    @Order(9)
    @DisplayName("POST /login → 200 OK with tokens")
    void shouldLoginSuccessfully() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();
    }

    @Test
    @Order(10)
    @DisplayName("POST /login wrong password → 401 Unauthorized")
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "WrongPass1@"}
                                """.formatted(TEST_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @Order(11)
    @DisplayName("POST /login non-existent user → 401 Unauthorized")
    void shouldRejectNonExistentUser() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody@test.com", "password": "%s"}
                                """.formatted(TEST_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    /* ═══════════════════════════════════════════
     *  Token Refresh
     * ═══════════════════════════════════════════ */

    @Test
    @Order(12)
    @DisplayName("POST /refresh-token → 200 with new tokens (rotation)")
    void shouldRefreshToken() throws Exception {
        // Login to get tokens
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        // Refresh
        mockMvc.perform(post("/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @Order(13)
    @DisplayName("POST /refresh-token invalid → 401")
    void shouldRejectInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "invalid-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    /* ═══════════════════════════════════════════
     *  Change Password (authenticated)
     * ═══════════════════════════════════════════ */

    @Test
    @Order(14)
    @DisplayName("POST /change-password → 200 OK")
    void shouldChangePassword() throws Exception {
        // Login to get JWT
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();

        // Change password
        String newPassword = "NewSecure1@";
        mockMvc.perform(post("/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}
                                """.formatted(TEST_PASSWORD, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Login with new password should work
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Restore original password for remaining tests
        MvcResult loginResult2 = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, newPassword)))
                .andReturn();
        String token2 = objectMapper.readTree(loginResult2.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(post("/change-password")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(newPassword, TEST_PASSWORD)));
    }

    @Test
    @Order(15)
    @DisplayName("POST /change-password without JWT → 401")
    void shouldRejectChangePasswordWithoutAuth() throws Exception {
        mockMvc.perform(post("/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "NewSecure1@"}
                                """.formatted(TEST_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    /* ═══════════════════════════════════════════
     *  Logout
     * ═══════════════════════════════════════════ */

    @Test
    @Order(16)
    @DisplayName("POST /logout → 200 OK")
    void shouldLogout() throws Exception {
        // Login to get refresh token
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        // Logout
        mockMvc.perform(post("/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully."));

        // Old refresh token should be invalid now
        mockMvc.perform(post("/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    /* ═══════════════════════════════════════════
     *  Empty / Missing fields
     * ═══════════════════════════════════════════ */

    @Test
    @Order(17)
    @DisplayName("POST /register empty fields → 400")
    void shouldRejectEmptyFields() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
