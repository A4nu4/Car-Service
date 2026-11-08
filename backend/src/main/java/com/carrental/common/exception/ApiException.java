package com.carrental.common.exception;

/**
 * The single exception type services throw for expected failures. The {@link ErrorCode}
 * decides the HTTP status; the message becomes the {@code detail} shown to API clients,
 * so it must never contain internal information.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    public static ApiException notFound(String resource, Object id) {
        return new ApiException(ErrorCode.NOT_FOUND, resource + " " + id + " was not found");
    }

    public static ApiException businessRule(String detail) {
        return new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, detail);
    }

    public static ApiException invalidTransition(String detail) {
        return new ApiException(ErrorCode.INVALID_STATE_TRANSITION, detail);
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(ErrorCode.FORBIDDEN, detail);
    }
}
