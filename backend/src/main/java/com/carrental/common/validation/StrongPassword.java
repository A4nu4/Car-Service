package com.carrental.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 8-100 characters containing at least one letter and one digit. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Size(min = 8, max = 100)
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$")
@jakarta.validation.ReportAsSingleViolation
public @interface StrongPassword {

    String message() default "must be 8-100 characters and contain at least one letter and one digit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
