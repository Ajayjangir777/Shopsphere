package com.shopsphere.order.feignClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** Subset of the Product Service's response that orders need; unknown fields are ignored (tolerant reader). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDto(UUID id, String name, BigDecimal price, Integer stock) {}
