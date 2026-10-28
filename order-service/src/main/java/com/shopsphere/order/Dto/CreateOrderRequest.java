package com.shopsphere.order.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

/**
 * What a client sends to place an order. There is deliberately NO price or total field:
 * prices always come from the Product Service, never from the caller.
 *
 * @param addressId id of one of the caller's saved addresses
 * @param items     1 to 10 lines
 */
public record CreateOrderRequest(
        @NotNull(message = "Address is required") UUID addressId,
        @NotEmpty(message = "At least one item is required")
        @Size(max = 10, message = "At most 10 items per order") List<@Valid Item> items
) {
    /** One requested line. */
    public record Item(
            @NotNull UUID productId,
            @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 100, message = "Quantity must be at most 100") int quantity) {}
}
