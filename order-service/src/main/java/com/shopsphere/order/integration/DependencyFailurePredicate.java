package com.shopsphere.order.integration;

import feign.FeignException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Decides whether an exception means "the dependency is unhealthy". Used by BOTH the
 * circuit breaker (does this call count as a failure?) and the retry (is trying again
 * worthwhile?).
 *
 * <p>Counts: server errors (5xx), connection and timeout problems (Feign reports
 * those with status -1), and plain I/O or timeout exceptions.
 * Does NOT count: 4xx answers (the dependency worked, the request was wrong or
 * unknown), our own ApiExceptions, and Resilience4j's own rejections (open circuit,
 * full bulkhead). Without this, a client asking for unknown products could open the
 * circuit and block every other customer.
 *
 * <p>Instantiated by Resilience4j through the public no-argument constructor.
 */
public class DependencyFailurePredicate implements Predicate<Throwable> {


    /**
     * Tests an exception.
     *
     * @param throwable the exception thrown by the protected call
     * @return true if it indicates a dependency problem, false if it is a normal outcome
     */
    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof FeignException feign) {
            return feign.status() == -1 || feign.status() >= 500;
        }
        return throwable instanceof IOException || throwable instanceof TimeoutException;
    }
}
