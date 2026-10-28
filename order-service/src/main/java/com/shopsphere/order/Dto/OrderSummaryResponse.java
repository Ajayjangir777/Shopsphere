package com.shopsphere.order.Dto;

import com.shopsphere.order.CustomerOrder;
import com.shopsphere.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Compact row for the order list; no items, so listing never triggers extra queries. */
public record OrderSummaryResponse(UUID id, OrderStatus status, BigDecimal totalAmount, Instant createdAt) {

    /** Converts an entity to its summary form. */
    public static OrderSummaryResponse from(CustomerOrder o) {
        return new OrderSummaryResponse(o.getId(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt());
    }
}