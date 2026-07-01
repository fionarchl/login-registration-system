package com.auth.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for password strength.
 *
 * <p>Enforces: min 8 chars, max 64 chars, at least one uppercase letter,
 * one lowercase letter, one digit, and one special character.</p>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password must be 8–64 characters and contain at least one uppercase letter, "
            + "one lowercase letter, one digit, and one special character (@#$%^&+=!*)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
