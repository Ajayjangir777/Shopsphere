package com.shopsphere.user.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


/** Address data sent by the client; there is no owner field, the owner always comes from the token. */
public record AddressRequest(@NotBlank @Size(max = 30) String label,
                             @NotBlank @Size(max = 100) String recipientName,
                             @NotBlank @Size(max = 150) String line1,
                             @Size(max = 150) String line2,
                             @NotBlank @Size(max = 80) String city,
                             @NotBlank @Size(max = 80) String state,
                             @NotBlank @Pattern(regexp = "^[A-Za-z0-9 -]{3,12}$", message = "Postal code is not valid") String postalCode,
                             @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a 2-letter ISO code, e.g. IN") String country,
                             @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must be 7-15 digits, optionally starting with +") String phone) {
}
