package com.shopsphere.order.Dto;

import java.math.BigDecimal;
import java.util.UUID;

/** One purchased line with its computed line total. */
public record Item(UUID productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
}
