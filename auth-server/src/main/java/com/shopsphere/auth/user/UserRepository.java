package com.shopsphere.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link AppUser}. Spring Data generates the implementation.
 */
public interface UserRepository extends JpaRepository<AppUser, UUID> {

    /**
     * Finds a user by login name; used during authentication.
     *
     * @param username the login name
     * @return the user, or empty if none exists
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Checks whether a login name is already taken.
     *
     * @param username the login name
     * @return true if a user with that name exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an email is already registered.
     *
     * @param email the (normalized) email address
     * @return true if a user with that email exists
     */
    boolean existsByEmail(String email);

}
