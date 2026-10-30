package com.shopsphere.order.integration;


import com.shopsphere.common.web.ApiException;
import com.shopsphere.order.feignClient.ProductClient;
import com.shopsphere.order.feignClient.ProductDto;
import com.shopsphere.order.feignClient.UserClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies that retry, circuit breaker, bulkhead and fallbacks behave as designed. It
 * loads only Spring AOP, Resilience4j and the two lookup beans; the Feign clients are
 * mocks, so no database, Eureka or network is involved.
 */
@SpringBootTest(classes = ResilienceWiringTest.TestConfig.class)
@TestPropertySource(locations = "classpath:resilience-test.properties")
public class ResilienceWiringTest {

    /** Minimal context: AOP proxies + Resilience4j auto-configuration + the beans under test. */
    @Configuration
    @ImportAutoConfiguration({AopAutoConfiguration.class, CircuitBreakerAutoConfiguration.class,
            RetryAutoConfiguration.class, BulkheadAutoConfiguration.class})
    @Import({ProductLookup.class, AddressLookup.class})
    static class TestConfig {}

    @MockBean
    ProductClient productClient;
    @MockBean
    UserClient userClient;
    @Autowired ProductLookup productLookup;
    @Autowired
    AddressLookup addressLookup;
    @Autowired
    CircuitBreakerRegistry registry;

    private final UUID id = UUID.randomUUID();
    private final ProductDto product = new ProductDto(id, "Phone", new BigDecimal("10.00"), 5);

    /** Breaker state is shared by all tests in this context, so start each test from CLOSED. */
    @BeforeEach
    void resetBreakers() {
        registry.getAllCircuitBreakers().forEach(io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);
    }

    /** A transient 503 followed by success is hidden from the caller by the retry. */
    @Test
    void transientServerError_isRetriedOnce_thenSucceeds() {
        when(productClient.getById(id)).thenThrow(FeignErrors.status(503)).thenReturn(product);

        assertThat(productLookup.getById(id)).isEqualTo(product);
        verify(productClient, times(2)).getById(id);
    }

    /** A connection failure is also retried once, and then reported as a clean 503. */
    @Test
    void persistentFailure_isRetriedOnce_thenReturns503() {
        when(productClient.getById(id)).thenThrow(FeignErrors.connectionRefused());

        assertThatThrownBy(() -> productLookup.getById(id)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(ex.getMessage()).contains("unavailable");
        });
        verify(productClient, times(2)).getById(id);
    }

    /** "Not found" is a business answer: mapped to 422 and never retried. */
    @Test
    void notFound_isMappedTo422_andNotRetried() {
        when(productClient.getById(id)).thenThrow(FeignErrors.status(404));

        assertThatThrownBy(() -> productLookup.getById(id)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(productClient, times(1)).getById(id);
    }

    /** Many business errors in a row must not open the circuit (guards the failure predicate config). */
    @Test
    void businessErrors_doNotOpenTheCircuit() {
        when(productClient.getById(id)).thenThrow(FeignErrors.status(404));

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> productLookup.getById(id)).isInstanceOf(ApiException.class);
        }
        assertThat(state("productService")).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    /** Enough failures open the circuit; afterwards calls fail fast WITHOUT touching the dependency. */
    @Test
    void repeatedFailures_openTheCircuit_thenCallsFailFast() {
        when(productClient.getById(id)).thenThrow(FeignErrors.status(503));

        for (int i = 0; i < 2; i++) {   // 2 calls x 2 attempts = 4 recorded failures = the window
            assertThatThrownBy(() -> productLookup.getById(id)).isInstanceOf(ApiException.class);
        }
        assertThat(state("productService")).isEqualTo(CircuitBreaker.State.OPEN);

        clearInvocations(productClient);
        assertThatThrownBy(() -> productLookup.getById(id)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(ex.getMessage()).contains("temporarily unavailable");
        });
        verifyNoInteractions(productClient);
    }

    /** In HALF_OPEN, successful probe calls close the circuit again. */
    @Test
    void halfOpenCircuit_closesAfterSuccessfulProbes() {
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker = registry.circuitBreaker("productService");
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();   // skip the 60s wait: we test the transition, not the clock
        when(productClient.getById(id)).thenReturn(product);

        productLookup.getById(id);
        productLookup.getById(id);

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    /** With one call stuck inside, a second concurrent call is rejected immediately as "busy". */
    @Test
    void bulkhead_rejectsCallsWhenFull() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(productClient.getById(id)).thenAnswer(invocation -> {
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);   // hold the first call open
            return product;
        });

        CompletableFuture<ProductDto> inFlight = CompletableFuture.supplyAsync(() -> productLookup.getById(id));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> productLookup.getById(id)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(ex.getMessage()).contains("busy");
        });

        release.countDown();
        assertThat(inFlight.get(5, TimeUnit.SECONDS)).isEqualTo(product);
    }

    /** A 403 from the User Service is an integration problem: 502, not retried, circuit untouched. */
    @Test
    void addressLookup_forbiddenFromUserService_mapsTo502_withoutRetry() {
        when(userClient.getMyAddress(id)).thenThrow(FeignErrors.status(403));

        assertThatThrownBy(() -> addressLookup.getMyAddress(id)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(userClient, times(1)).getMyAddress(id);
        assertThat(state("userService")).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    /** An unknown (or foreign) address becomes 422. */
    @Test
    void addressLookup_notFound_mapsTo422() {
        when(userClient.getMyAddress(id)).thenThrow(FeignErrors.status(404));

        assertThatThrownBy(() -> addressLookup.getMyAddress(id)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    /** Current state of the named circuit breaker. */
    private CircuitBreaker.State state(String name) {
        return registry.circuitBreaker(name).getState();
    }

}
