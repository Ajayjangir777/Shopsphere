package com.shopsphere.user.Dto;

import com.shopsphere.user.UserProfile;

import java.util.UUID;

/**
 * Profile data returned to the client.
 *
 * @param id       profile id
 * @param username the token subject that owns it
 * @param fullName display name
 * @param phone    phone number, may be null
 */
public record ProfileRequest(UUID id, String username, String fullName, String phone) {


}
