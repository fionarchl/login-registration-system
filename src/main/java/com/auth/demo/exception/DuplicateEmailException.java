package com.auth.demo.exception;

/**
 * Thrown when a registration attempt uses an email that is already taken.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
