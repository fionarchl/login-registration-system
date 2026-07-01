package com.auth.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator for the {@link StrongPassword} constraint.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Length between 8 and 64 characters</li>
 *   <li>At least one uppercase letter (A–Z)</li>
 *   <li>At least one lowercase letter (a–z)</li>
 *   <li>At least one digit (0–9)</li>
 *   <li>At least one special character (@#$%^&amp;+=!*)</li>
 * </ul>
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[\\@#$%^&+=!*]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            // Let @NotBlank handle null/blank — don't double-report
            return true;
        }

        if (password.length() < 8 || password.length() > 64) {
            return false;
        }

        return UPPERCASE.matcher(password).find()
                && LOWERCASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL.matcher(password).find();
    }
}
