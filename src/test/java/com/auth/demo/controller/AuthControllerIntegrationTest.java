package com.auth.demo.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that boot the full Spring context and send real HTTP requests
 * to the authentication endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /* ── Registration ── */

    @Test
    @Order(1)
    @DisplayName("POST /register → 201 Created")
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "integration@test.com")
                        .param("password", "securepassword"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /register duplicate → 409 Conflict")
    void shouldRejectDuplicateRegistration() throws Exception {
        // First registration
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "duplicate@test.com")
                .param("password", "securepassword"));

        // Duplicate attempt
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "duplicate@test.com")
                        .param("password", "anotherpassword"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    @Order(3)
    @DisplayName("POST /register invalid email → 400 Bad Request")
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "not-an-email")
                        .param("password", "securepassword"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(4)
    @DisplayName("POST /register short password → 400 Bad Request")
    void shouldRejectShortPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "short@test.com")
                        .param("password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(5)
    @DisplayName("POST /register empty fields → 400 Bad Request")
    void shouldRejectEmptyFields() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "")
                        .param("password", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /* ── Login ── */

    @Test
    @Order(6)
    @DisplayName("POST /login → 200 OK")
    void shouldLoginSuccessfully() throws Exception {
        // Register first
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "login@test.com")
                .param("password", "securepassword"));

        // Login
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "login@test.com")
                        .param("password", "securepassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful."));
    }

    @Test
    @Order(7)
    @DisplayName("POST /login wrong password → 401 Unauthorized")
    void shouldRejectWrongPassword() throws Exception {
        // Register first
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "wrongpw@test.com")
                .param("password", "securepassword"));

        // Login with wrong password
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "wrongpw@test.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @Order(8)
    @DisplayName("POST /login non-existent user → 401 Unauthorized")
    void shouldRejectNonExistentUser() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "nobody@test.com")
                        .param("password", "securepassword"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
