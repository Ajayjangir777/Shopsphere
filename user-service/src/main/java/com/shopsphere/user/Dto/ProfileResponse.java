package com.shopsphere.user.Dto;


import com.shopsphere.user.UserProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Profile data sent by the client.
 *
 * @param fullName display name
 * @param phone    optional phone number with optional leading +
 */
public record ProfileResponse(
        UUID id, String username, String fullName, String phone
) {
    /** Converts an entity to its response form. */
    public static ProfileResponse from(UserProfile p) {
        return new ProfileResponse(p.getId(), p.getOwnerSub(), p.getFullName(), p.getPhone());
    }
}
