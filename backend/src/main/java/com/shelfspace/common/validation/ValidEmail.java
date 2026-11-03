package com.shelfspace.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Stricter than @Email: needs a dot and a 2+ letter ending after the @ (so "name@gmail" is refused). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEmailValidator.class)
public @interface ValidEmail {

    String message() default "must be a valid email address, like name@example.com";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
