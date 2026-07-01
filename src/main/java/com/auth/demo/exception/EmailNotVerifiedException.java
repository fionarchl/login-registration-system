package com.auth.demo.exception;

/**
 * Thrown when an unverified user attempts to login.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
