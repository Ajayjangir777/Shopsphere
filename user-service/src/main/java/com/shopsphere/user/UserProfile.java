package com.shopsphere.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A customer's profile. One row per user, identified by the token's "sub" claim. */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserProfile {

    /** Generated identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The JWT subject that owns this profile; unique. */
    @Column(name = "owner_sub", nullable = false, unique = true, length = 100)
    private String ownerSub;

    /** Display name. */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Optional contact number. */
    @Column(length = 20)
    private String phone;

    /** Filled automatically on insert. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Refreshed automatically on update. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}