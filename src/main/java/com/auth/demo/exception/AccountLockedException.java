package com.auth.demo.exception;

/**
 * Thrown when a locked account attempts to login.
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }
}
