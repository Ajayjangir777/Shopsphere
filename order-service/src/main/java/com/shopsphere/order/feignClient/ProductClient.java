package com.shopsphere.order.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Declarative HTTP client for the Product Service. "product-service" is the Eureka name,
 * so no host or port is configured. Public endpoint: no token is sent.
 */
@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {

    /** GET /api/v1/products/{id}: throws FeignException.NotFound for an unknown id. */
    @GetMapping("/{id}")
    ProductDto getById(@PathVariable("id") UUID id);
}