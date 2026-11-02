package com.shelfspace.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class PastOrNextYearValidator implements ConstraintValidator<PastOrNextYear, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || value <= Year.now().getValue() + 1;
    }
}
