package com.shopsphere.order;

/**
 * Lifecycle states of an order. Only CREATED and CANCELLED exist for now; payment and
 * shipping states (PAYMENT_PENDING, PAID, SHIPPED, DELIVERED) arrive with the Saga step.
 */
public enum OrderStatus {
    CREATED, CANCELLED
}
