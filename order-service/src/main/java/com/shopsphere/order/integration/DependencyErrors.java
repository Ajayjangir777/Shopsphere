package com.shopsphere.order.integration;

import com.shopsphere.common.web.ApiException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

/**
 * Builds the client-facing errors used by the fallback methods, so wording and logging
 * are the same for every dependency. All of them are {@link ApiException}s, so the shared
 * advice turns them into clean RFC 7807 responses (503 ones with Retry-After).
 */
@Slf4j
public class DependencyErrors {

    private DependencyErrors() {}

    /**
     * The circuit is open: we are deliberately not calling the dependency.
     *
     * @param dependency display name, e.g. "Product service"
     * @return a 503 error
     */
    static ApiException circuitOpen(String dependency) {
        log.warn("{}: circuit is OPEN, failing fast", dependency);
        return ApiException.unavailable(dependency + " is temporarily unavailable. Please retry shortly.");
    }

    /**
     * Too many calls to this dependency are already in flight.
     *
     * @param dependency display name
     * @return a 503 error
     */
    static ApiException busy(String dependency) {
        log.warn("{}: bulkhead full, rejecting call", dependency);
        return ApiException.unavailable(dependency + " is busy. Please retry shortly.");
    }

    /**
     * Translates a failed Feign call that survived the retry.
     * 5xx, timeouts and connection failures mean "dependency down" (503); any other
     * unexpected status (for example 401/403 from a bad token or scope) is an integration
     * problem on our side, reported as 502 without leaking details.
     *
     * @param dependency display name
     * @param ex         the Feign exception
     * @return a 503 or 502 error
     */
    static ApiException fromFeign(String dependency, FeignException ex) {
        log.warn("{} call failed: status={}", dependency, ex.status());
        if (ex.status() == -1 || ex.status() >= 500) {
            return ApiException.unavailable(dependency + " is unavailable. Please retry shortly.");
        }
        return new ApiException(HttpStatus.BAD_GATEWAY, "Unexpected response from a dependent service");
    }

}
