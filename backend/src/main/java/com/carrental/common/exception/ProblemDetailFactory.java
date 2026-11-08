package com.carrental.common.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Builds every error body in one place so controllers, the exception handler and the
 * security filters all return exactly the same JSON shape.
 */
@Component
public class ProblemDetailFactory {

    private static final String TYPE_BASE = "https://api.car-rental.example/errors/";

    private final Clock clock;

    public ProblemDetailFactory(Clock clock) {
        this.clock = clock;
    }

    public ProblemDetail create(ErrorCode code, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setTitle(code.title());
        problem.setType(typeFor(code));
        if (path != null) {
            problem.setInstance(URI.create(path));
        }
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now(clock));
        return problem;
    }

    public ProblemDetail validation(List<FieldError> errors, String path) {
        ProblemDetail problem = create(ErrorCode.VALIDATION_ERROR, "One or more fields are invalid", path);
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Adds our {@code code}/{@code timestamp} properties to a ProblemDetail created by Spring itself. */
    public ProblemDetail enrich(ProblemDetail problem) {
        ErrorCode code = ErrorCode.fromStatus(problem.getStatus());
        problem.setType(typeFor(code));
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now(clock));
        return problem;
    }

    private static URI typeFor(ErrorCode code) {
        return URI.create(TYPE_BASE + code.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }
}
