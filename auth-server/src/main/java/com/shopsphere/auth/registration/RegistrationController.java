package com.shopsphere.auth.registration;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST endpoint for self-service sign-up. No authentication is required,
 * because the caller does not have an account yet.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {


    /** Performs the actual registration. */
    private final UserRegistrationService registrationService;

    /**
     * Creates a customer account. Returns 201 on success; the user then logs in through
     * the normal OAuth2 flow (we do not log them in automatically).
     *
     * @param request the sign-up data; validated before this method runs
     * @return 201 Created with the new user's id, username and email
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }
}
