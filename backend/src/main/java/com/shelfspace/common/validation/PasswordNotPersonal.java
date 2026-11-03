package com.shelfspace.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** On a request record with name, email and password components: the password must not contain the name or email name. The error is reported on the password field. */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordNotPersonalValidator.class)
public @interface PasswordNotPersonal {

    String message() default "must not contain your name or email";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
