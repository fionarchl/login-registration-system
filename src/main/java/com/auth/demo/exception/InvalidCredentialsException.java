package com.auth.demo.exception;

/**
 * Thrown when login credentials (email or password) do not match any registered user.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
