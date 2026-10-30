package com.shopsphere.order.integration;


import com.shopsphere.common.web.ApiException;
import com.shopsphere.order.feignClient.ProductClient;
import com.shopsphere.order.feignClient.ProductDto;
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
 * Resilient access to the Product Service. It wraps {@link ProductClient} and adds retry,
 * circuit breaker and bulkhead through annotations configured under the name
 * "productService" in order-service.yml.
 *
 * <p>This must be a separate bean: Resilience4j works through Spring proxies, so a call
 * from another method of the SAME class would bypass all protection.
 *
 * <p>Order of protection (outermost first): Retry, CircuitBreaker, Bulkhead, then the call.
 * Each retry attempt therefore passes through the breaker and is counted by it.
 */
@Component
@RequiredArgsConstructor
public class ProductLookup {

    /** Name of the Resilience4j instance (must match order-service.yml). */
    private static final String INSTANCE = "productService";

    /** Name used in error messages. */
    private static final String NAME = "Product service";

    /** The plain Feign client doing the HTTP call. */
    private final ProductClient productClient;

    /**
     * Fetches a product (price, name, stock). Safe to retry because it is a GET.
     * The only fallback is attached to the outermost annotation (@Retry), so it handles
     * whatever finally comes out of the whole chain.
     *
     * @param productId the product id
     * @return the product
     * @throws ApiException 422 if the product does not exist, 503 if the service is
     *                      down, open-circuit or busy
     */
    @Retry(name = INSTANCE, fallbackMethod = "fallback")
    @CircuitBreaker(name = INSTANCE)
    @Bulkhead(name = INSTANCE)
    public ProductDto getById(UUID productId) {
        try {
            return productClient.getById(productId);
        } catch (FeignException.NotFound ex) {
            // A normal business answer: translate it HERE so the breaker and retry see an
            // ApiException (not a failure, not retried) instead of a Feign error.
            throw ApiException.unprocessable("Product not found: " + productId);
        }
    }

    /**
     * Fallback when the circuit is open. Invoked reflectively by Resilience4j: the method
     * with the most specific exception type wins, and the signature must be the original
     * parameters plus the exception.
     */
    protected ProductDto fallback(UUID productId, CallNotPermittedException ex) {
        throw DependencyErrors.circuitOpen(NAME);
    }

    /** Fallback when too many calls are already in flight. */
    protected ProductDto fallback(UUID productId, BulkheadFullException ex) {
        throw DependencyErrors.busy(NAME);
    }

    /** Fallback for a Feign failure that survived the retry (5xx, timeout, connection refused, odd 4xx). */
    protected ProductDto fallback(UUID productId, FeignException ex) {
        throw DependencyErrors.fromFeign(NAME, ex);
    }

    /**
     * Catch-all that lets every other runtime exception through untouched, including our
     * own ApiException (the 422 above). Without it, the more general handlers would be the
     * only candidates and a business error could be rewritten into a misleading 503.
     */
    protected ProductDto fallback(UUID productId, RuntimeException ex) {
        throw ex;
    }
}
