package com.shopsphere.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A saved delivery address, always scoped to one owner. */
@Entity
@Table(name = "addresses")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Address {

        /** Generated identifier. */
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        /** The JWT subject that owns this address. */
        @Column(name = "owner_sub", nullable = false, length = 100)
        private String ownerSub;

        @Column(nullable = false, length = 30) private String label;
        @Column(name = "recipient_name", nullable = false, length = 100) private String recipientName;
        @Column(nullable = false, length = 150) private String line1;
        @Column(length = 150) private String line2;
        @Column(nullable = false, length = 80) private String city;
        @Column(nullable = false, length = 80) private String state;
        @Column(name = "postal_code", nullable = false, length = 12) private String postalCode;
        @Column(nullable = false, length = 2) private String country;
        @Column(length = 20) private String phone;

        /** Filled automatically on insert. */
        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        /** Refreshed automatically on update. */
        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private Instant updatedAt;

}
