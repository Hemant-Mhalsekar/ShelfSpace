package com.shelfspace.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.RecordComponent;

public class PasswordNotPersonalValidator implements ConstraintValidator<PasswordNotPersonal, Record> {

    @Override
    public boolean isValid(Record value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String password = component(value, "password");
        return PasswordPolicy.personalProblem(password, component(value, "name"), component(value, "email"))
                .map(message -> {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(message).addPropertyNode("password").addConstraintViolation();
                    return false;
                }).orElse(true);
    }

    private static String component(Record r, String name) {
        try {
            for (RecordComponent c : r.getClass().getRecordComponents()) {
                if (c.getName().equals(name)) {
                    return (String) c.getAccessor().invoke(r);
                }
            }
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
