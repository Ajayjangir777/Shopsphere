package com.shopsphere.auth.registration;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of the sign-up request. It deliberately has NO role field: clients must never
 * decide their own privileges, so every self-registered user becomes a CUSTOMER.
 *
 * @param username login name: 3-30 letters, digits or underscores
 * @param email    contact address, must look like an email
 * @param password plain password; 8-72 characters (BCrypt ignores anything past 72 bytes)
 */
public record RegistrationRequest(
        @NotBlank(message = "Username is required") @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "Username must be 3-30 characters: letters, digits or underscore") String username,

        @NotBlank(message = "Email is required") @Email(message = "Email is not valid") @Size(max = 150, message = "Email must be at most 150 characters") String email,

        @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {

    /**
     * Hides the password so it can never leak into logs if this object is printed.
     * (A record's default toString would include every field.)
     *
     * @return a safe text form of the request
     */
    @Override
    public String toString() {
        return "RegistrationRequest[username=" + username + ", email=" + email + ", password=***]";
    }
}
