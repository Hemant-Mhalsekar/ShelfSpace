package com.shelfspace.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PersonNameValidator implements ConstraintValidator<PersonName, String> {

    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L}\\p{M} .'-]*$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;   // @NotBlank reports missing values
        }
        String name = value.strip();
        return name.length() >= 2 && name.length() <= 100 && NAME.matcher(name).matches();
    }
}
