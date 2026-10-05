package com.shopsphere.auth.registration;

import java.util.UUID;

/**
 * What the API returns after a successful sign-up. It never contains the password
 * or its hash.
 *
 * @param id       identifier of the new user
 * @param username the stored (normalized) login name
 * @param email    the stored (normalized) email
 */
public record RegistrationResponse(UUID id, String username, String email) {


}
