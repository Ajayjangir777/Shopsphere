package com.shopsphere.order.feignClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Subset of the User Service's address response that orders need. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AddressDto(UUID id, String recipientName, String line1, String line2, String city,
                         String state, String postalCode, String country, String phone) {}
