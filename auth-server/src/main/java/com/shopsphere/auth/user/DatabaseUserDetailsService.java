package com.shopsphere.auth.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Bridges our database to Spring Security: when someone submits the login form,
 * Spring Security calls this class to load the user and then compares the submitted
 * password against the stored hash itself.
 */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    /** Source of user records. */
    private final UserRepository userRepository;

    /**
     * Loads a user and converts it to Spring Security's {@link UserDetails}.
     * Roles become authorities with the ROLE_ prefix (ADMIN becomes ROLE_ADMIN),
     * which is the convention used by {@code hasRole("ADMIN")} checks.
     *
     * @param username the login name entered by the user
     * @return the user details used for password checking and authorities
     * @throws UsernameNotFoundException if no such user exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Usernames are stored lower-case, so "Alice" typed at the login form must find "alice".
        AppUser user = userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(authorities)
                .build();
    }
}
