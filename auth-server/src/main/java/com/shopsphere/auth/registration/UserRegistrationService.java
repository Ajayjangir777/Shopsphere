package com.shopsphere.auth.registration;

import com.shopsphere.auth.user.AppUser;
import com.shopsphere.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Business logic for creating a new customer account: normalizes input, rejects
 * duplicates, hashes the password and stores the user with the CUSTOMER role.
 */
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    /** Role given to everyone who signs up through the public API. */
    private static final String DEFAULT_ROLE = "CUSTOMER";

    /** Persistence for users. */
    private final UserRepository userRepository;

    /** Hashes passwords; the same encoder Spring Security uses to check them at login. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new customer.
     *
     * <p>Duplicates are checked up front for friendly messages, but the database unique
     * constraints are the real guard: two simultaneous requests can both pass the check,
     * and only the constraint stops the second insert. {@code saveAndFlush} forces the
     * INSERT to run inside this method so that violation can be caught here.
     *
     * @param request the validated sign-up data
     * @return the created user's public details
     * @throws DuplicateUserException if the username or email is already registered
     */
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String username = normalize(request.username());
        String email = normalize(request.email());

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUserException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("Email is already registered");
        }

        AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(Set.of(DEFAULT_ROLE)))
                .build();

        try {
            AppUser saved = userRepository.saveAndFlush(user);
            return new RegistrationResponse(saved.getId(), saved.getUsername(), saved.getEmail());
        } catch (DataIntegrityViolationException ex) {
            // Lost a race with a concurrent registration of the same username or email.
            throw new DuplicateUserException("Username or email is already registered");
        }
    }

    /**
     * Trims and lower-cases a value so "Alice" and "alice " count as the same account.
     * Without this, someone could register a look-alike of an existing name.
     *
     * @param value raw input
     * @return the normalized value
     */
    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
