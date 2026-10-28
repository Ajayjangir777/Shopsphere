package com.shopsphere.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /** Finds the profile owned by the given subject. */
    Optional<UserProfile> findByOwnerSub(String ownerSub);
}
