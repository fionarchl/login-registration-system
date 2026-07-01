package com.auth.demo.util;

/**
 * Utility class for sanitizing user-supplied input.
 *
 * <p>All methods are null-safe and return {@code null} when given {@code null}.</p>
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // Utility class — prevent instantiation
    }

    /**
     * Normalises an email address:
     * <ol>
     *   <li>Trims leading/trailing whitespace</li>
     *   <li>Converts to lowercase</li>
     *   <li>Strips HTML tags to guard against stored XSS</li>
     * </ol>
     */
    public static String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return stripHtmlTags(email.trim().toLowerCase());
    }

    /**
     * Validates that the email address contains both {@code @} and {@code .}.
     *
     * @throws IllegalArgumentException if the email is missing {@code @} or {@code .}
     */
    public static void validateEmailFormat(String email) {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Email must contain '@' and '.'");
        }
    }

    /**
     * Removes HTML/XML tags and loose angle-bracket characters.
     */
    public static String stripHtmlTags(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "")
                    .replaceAll("[<>]", "");
    }
}
