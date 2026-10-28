package com.shopsphere.user.controller;


import com.shopsphere.user.Dto.AddressRequest;
import com.shopsphere.user.Dto.AddressResponse;
import com.shopsphere.user.Dto.ProfileRequest;
import com.shopsphere.user.Dto.ProfileResponse;
import com.shopsphere.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST API for the caller's own data. The path is /me: the identity always comes from the
 * verified token (@AuthenticationPrincipal), never from a URL or body field.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Returns the caller's profile. */
    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(jwt.getSubject());
    }

    /** Creates or replaces the caller's profile. */
    @PutMapping
    public ProfileResponse saveProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest request) {
        return userService.saveProfile(jwt.getSubject(), request);
    }

    /** Lists the caller's addresses. */
    @GetMapping("/addresses")
    public List<AddressResponse> listAddresses(@AuthenticationPrincipal Jwt jwt) {
        return userService.listAddresses(jwt.getSubject());
    }

    /** Returns one address (the Order Service calls this with the user's relayed token). */
    @GetMapping("/addresses/{id}")
    public AddressResponse getAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return userService.getAddress(jwt.getSubject(), id);
    }

    /** Adds an address; 201 with a Location header. */
    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody AddressRequest request) {
        AddressResponse created = userService.addAddress(jwt.getSubject(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Deletes an address; 204. */
    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        userService.deleteAddress(jwt.getSubject(), id);
    }

}
