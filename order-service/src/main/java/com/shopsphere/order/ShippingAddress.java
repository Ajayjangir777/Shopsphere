package com.shopsphere.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** Copy of the delivery address taken when the order was placed (embedded in the orders table). */
@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ShippingAddress {

    @Column(name = "ship_recipient_name", nullable = false, length = 100) private String recipientName;
    @Column(name = "ship_line1", nullable = false, length = 150) private String line1;
    @Column(name = "ship_line2", length = 150) private String line2;
    @Column(name = "ship_city", nullable = false, length = 80) private String city;
    @Column(name = "ship_state", nullable = false, length = 80) private String state;
    @Column(name = "ship_postal_code", nullable = false, length = 12) private String postalCode;
    @Column(name = "ship_country", nullable = false, length = 2) private String country;
    @Column(name = "ship_phone", length = 20) private String phone;
}