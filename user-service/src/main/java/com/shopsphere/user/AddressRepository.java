package com.shopsphere.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    /** All of an owner's addresses, oldest first. */
    List<Address> findByOwnerSubOrderByCreatedAtAsc(String ownerSub);

    /** One address, but only if it belongs to the given owner. */
    Optional<Address> findByIdAndOwnerSub(UUID id, String ownerSub);

    /** How many addresses an owner has (used for the per-user limit). */
    long countByOwnerSub(String ownerSub);

    /** Deletes an address only if owned by the given owner; returns how many rows were deleted (0 or 1). */
    long deleteByIdAndOwnerSub(UUID id, String ownerSub);
}
