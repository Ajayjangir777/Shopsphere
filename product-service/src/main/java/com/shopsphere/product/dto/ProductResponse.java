package com.shopsphere.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(UUID id, String name, String description, BigDecimal price, String category,
                              Integer stock, Instant createdAt, Instant updatedAt) {


}
