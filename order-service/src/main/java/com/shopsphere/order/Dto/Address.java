package com.shopsphere.order.Dto;


/** Address snapshot as shown to the client. */
public record Address(String recipientName, String line1, String line2, String city, String state,
                      String postalCode, String country, String phone) {
}
