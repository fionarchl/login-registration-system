package com.auth.demo.service;

/**
 * Contract for sending emails.
 */
public interface EmailService {

    /**
     * Sends a verification email containing a link the user must click to confirm their address.
     *
     * @param to    the recipient's email address
     * @param token the unique verification token
     */
    void sendVerificationEmail(String to, String token);
}
