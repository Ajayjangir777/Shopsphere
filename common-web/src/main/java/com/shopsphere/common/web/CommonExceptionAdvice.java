package com.shopsphere.common.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One error-handling policy for every service. It extends Spring's
 * {@link ResponseEntityExceptionHandler}, which already maps Spring MVC's own exceptions
 * correctly (malformed JSON gives 400, wrong method 405, unknown path 404), and adds
 * our application errors. Order 0 keeps it ahead of any other advice.
 */
@RestControllerAdvice
@Order(0)
public class CommonExceptionAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionAdvice.class);

    /**
     * Our deliberate errors: status and message come from the exception itself.
     * For 503 we also send a Retry-After header, the standard way to tell clients how long
     * to back off before trying again (a hint, not a guarantee).
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getStatus().getReasonPhrase());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus());
        if (ex.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            response.header(HttpHeaders.RETRY_AFTER, "5");
        }
        return response.body(problem);
    }

    /** A @PreAuthorize check failed inside a controller: 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action");
        problem.setTitle("Access denied");
        return problem;
    }

    /** Invalid @RequestParam / @PathVariable values (from class-level @Validated): 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request parameter");
        return problem;
    }

    /** Concurrent modification or a unique-constraint race: 409. Database details are never exposed. */
    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ProblemDetail handleConflict(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request conflicts with the current state of the data. Reload and retry.");
        problem.setTitle("Conflict");
        return problem;
    }

    /** Last resort for bugs: log everything, tell the client nothing internal. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong");
        problem.setTitle("Internal server error");
        return problem;
    }

    /** Body validation failed (@Valid): 400 with one message per field, never the rejected values. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ex.getBody();
        problem.setTitle("Invalid request");
        problem.setDetail("Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

}
