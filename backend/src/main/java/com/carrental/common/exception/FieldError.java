package com.carrental.common.exception;

/** One invalid input field, listed in the {@code errors} array of a VALIDATION_ERROR response. */
public record FieldError(String field, String message) {
}
