package com.shopsphere.order.integration;


import com.shopsphere.common.web.ApiException;
import com.shopsphere.order.feignClient.AddressDto;
import com.shopsphere.order.feignClient.UserClient;
import feign.FeignException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resilient access to the User Service for the caller's saved addresses. Same protection
 * as {@link ProductLookup} but with its OWN breaker, retry and bulkhead ("userService"),
 * so trouble with one dependency never trips the other.
 *
 * <p>The caller's token is relayed by the Feign interceptor, which reads a thread-local.
 * Resilience4j's semaphore bulkhead and retry run on the request thread, so that keeps
 * working. A thread-pool bulkhead or async time limiter would NOT carry the token.
 */
@Component
@RequiredArgsConstructor
public class AddressLookup {


    /** Name of the Resilience4j instance (must match order-service.yml). */
    private static final String INSTANCE = "userService";

    /** Name used in error messages. */
    private static final String NAME = "User service";

    /** The plain Feign client doing the HTTP call (with token relay). */
    private final UserClient userClient;

    /**
     * Fetches one of the caller's addresses. Safe to retry because it is a GET.
     *
     * @param addressId the address id
     * @return the address
     * @throws ApiException 422 if it does not exist or is not the caller's, 503/502 on dependency trouble
     */
    @Retry(name = INSTANCE, fallbackMethod = "fallback")
    @CircuitBreaker(name = INSTANCE)
    @Bulkhead(name = INSTANCE)
    public AddressDto getMyAddress(UUID addressId) {
        try {
            return userClient.getMyAddress(addressId);
        } catch (FeignException.NotFound ex) {
            // Same answer whether missing or owned by someone else: no information leak.
            throw ApiException.unprocessable("Address not found: " + addressId);
        }
    }

    /** Fallback when the circuit is open. */
    protected AddressDto fallback(UUID addressId, CallNotPermittedException ex) {
        throw DependencyErrors.circuitOpen(NAME);
    }

    /** Fallback when too many calls are already in flight. */
    protected AddressDto fallback(UUID addressId, BulkheadFullException ex) {
        throw DependencyErrors.busy(NAME);
    }

    /** Fallback for a Feign failure that survived the retry. */
    protected AddressDto fallback(UUID addressId, FeignException ex) {
        throw DependencyErrors.fromFeign(NAME, ex);
    }

    /** Lets every other runtime exception (including ApiException) through unchanged. */
    protected AddressDto fallback(UUID addressId, RuntimeException ex) {
        throw ex;
    }
}
