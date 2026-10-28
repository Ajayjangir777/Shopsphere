package com.shopsphere.user.Dto;

import com.shopsphere.user.Address;

import java.util.UUID;

/** Address data returned to the client (also the contract the Order Service reads through Feign). */
public record AddressResponse(UUID id, String label, String recipientName, String line1, String line2,
                              String city, String state, String postalCode, String country, String phone) {

    /** Converts an entity to its response form. */
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getRecipientName(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPostalCode(), a.getCountry(), a.getPhone());
    }
}
