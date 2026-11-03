package com.shelfspace.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A person's name: 2-100 characters after trimming, starting with a letter; letters (any language), spaces, ' - and . only. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PersonNameValidator.class)
public @interface PersonName {

    String message() default "must be 2-100 characters: letters, spaces, apostrophes, hyphens and dots";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
