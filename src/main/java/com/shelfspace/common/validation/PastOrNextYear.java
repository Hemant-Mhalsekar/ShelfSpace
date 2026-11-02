package com.shelfspace.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A year no later than next year (books can be announced a little ahead). Null is allowed. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PastOrNextYearValidator.class)
public @interface PastOrNextYear {

    String message() default "can't be later than next year";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
