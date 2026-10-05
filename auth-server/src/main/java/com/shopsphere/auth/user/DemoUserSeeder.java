package com.shopsphere.auth.user;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates two demo accounts on startup so the system can be tried immediately.
 * Runs only when {@code auth.seed-demo-users=true}; it must stay off in production
 * because the passwords are well known.
 *
 * <p>Hashing happens here at runtime, which is why the Flyway migration contains
 * no user rows (we never hand-write password hashes into SQL).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.seed-demo-users", havingValue = "true")
public class DemoUserSeeder implements ApplicationRunner {

    /** Used to check for and save users. */
    private final UserRepository userRepository;

    /** Hashes the demo passwords. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Seeds the demo users after the application has started.
     *
     * @param args application arguments (unused)
     */

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createIfMissing("admin", "admin@shopsphere.local", "admin123", Set.of("ADMIN"));
        createIfMissing("customer", "customer@shopsphere.local", "customer123", Set.of("CUSTOMER"));
    }

    /**
     * Inserts one user unless the username already exists (so restarts are harmless).
     *
     * @param username    login name
     * @param email       contact email
     * @param rawPassword plain password, hashed before saving
     * @param roles       role names without the ROLE_ prefix
     */
    private void createIfMissing(String username, String email, String rawPassword, Set<String> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(AppUser.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .roles(new HashSet<>(roles))
                .build());
        log.info("Seeded demo user '{}' with roles {}", username, roles);
    }
}
