package com.auth.demo.exception;

/**
 * Thrown when login rate limit is exceeded for an IP address.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
