package com.auth.demo.controller;

import com.auth.demo.dto.ApiResponse;
import com.auth.demo.dto.AuthRequest;
import com.auth.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints.
 *
 * <p>Both endpoints accept {@code application/x-www-form-urlencoded} data.
 * Spring's implicit {@code @ModelAttribute} binding maps form fields
 * ({@code email} and {@code password}) directly onto {@link AuthRequest}.</p>
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account.
     *
     * <p><strong>POST /register</strong><br>
     * Content-Type: application/x-www-form-urlencoded<br>
     * Fields: {@code email}, {@code password}</p>
     *
     * @param request validated form data
     * @return 201 Created on success; 400/409 on validation or duplicate errors
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid AuthRequest request) {
        ApiResponse response = authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user with email and password.
     *
     * <p><strong>POST /login</strong><br>
     * Content-Type: application/x-www-form-urlencoded<br>
     * Fields: {@code email}, {@code password}</p>
     *
     * @param request validated form data
     * @return 200 OK on success; 401 Unauthorized on bad credentials
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid AuthRequest request) {
        ApiResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }
}
