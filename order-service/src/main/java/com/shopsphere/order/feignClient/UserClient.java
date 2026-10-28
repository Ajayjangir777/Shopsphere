package com.shopsphere.order.feignClient;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Declarative HTTP client for the User Service. It carries the caller's token (token relay),
 * so the User Service can verify that the address belongs to that user.
 */
@FeignClient(name = "user-service", path = "/api/v1/users/me", configuration = RelayTokenFeignConfig.class)
public interface UserClient {

    /** GET /me/addresses/{id}: 404 if the address does not exist OR belongs to someone else. */
    @GetMapping("/addresses/{id}")
    AddressDto getMyAddress(@PathVariable("id") UUID id);
}