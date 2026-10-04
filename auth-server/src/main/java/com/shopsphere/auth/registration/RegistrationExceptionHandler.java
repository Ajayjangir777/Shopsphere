package com.shopsphere.auth.registration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts registration failures into consistent RFC 7807 error responses
 * (the same format used by the Product Service).
 */
@RestControllerAdvice
public class RegistrationExceptionHandler extends RuntimeException {
    /**
     * Maps a taken username or email to 409 Conflict.
     *
     * @param ex the duplicate-user exception
     * @return a problem document with the reason
     */
    @ExceptionHandler(DuplicateUserException.class)
    public ProblemDetail handleDuplicate(DuplicateUserException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("User already exists");
        return problem;
    }

    /**
     * Maps bean-validation failures to 400 Bad Request with one message per bad field.
     * Only the messages are returned, never the rejected values (a password must not be echoed).
     *
     * @param ex the validation exception raised for the request body
     * @return a problem document with an "errors" map keyed by field name
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return problem;
    }
}
