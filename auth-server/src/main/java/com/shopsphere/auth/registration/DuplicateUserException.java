package com.shopsphere.auth.registration;

/**
 * Thrown when the username or email is already used by another account.
 * Translated to HTTP 409 Conflict by {@link RegistrationExceptionHandler}.
 */
public class DuplicateUserException extends RuntimeException {

    /**
     * Creates the exception with a message that is safe to show to the client.
     *
     * @param message explanation of which value is taken
     */
    public DuplicateUserException(String message) {
        super(message);
    }
}
