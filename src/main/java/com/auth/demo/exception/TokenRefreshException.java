package com.auth.demo.exception;

/**
 * Thrown for expired or invalid refresh tokens.
 */
public class TokenRefreshException extends RuntimeException {

    public TokenRefreshException(String message) {
        super(message);
    }
}
