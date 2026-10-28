package com.shopsphere.order.repository;

import com.shopsphere.order.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Data access for orders. Every lookup is scoped to the owner. */
public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    /** One page of an owner's orders. Items are NOT fetched here (see concepts: paging + collections). */
    Page<CustomerOrder> findByOwnerSub(String ownerSub, Pageable pageable);

    /** One order of the owner with its items loaded in the same query; empty if it is someone else's. */
    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findByIdAndOwnerSub(UUID id, String ownerSub);
}