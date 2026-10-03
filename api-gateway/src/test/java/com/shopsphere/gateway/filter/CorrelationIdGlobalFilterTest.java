package com.shopsphere.gateway.filter;


import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Unit tests for {@link CorrelationIdGlobalFilter}. They use Spring's mock
 * exchange and a stub filter chain, so no server, Eureka or Redis is required.
 */
public class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    /** Without an incoming header, the filter must create an ID and forward it. */
    @Test
    void generatesIdWhenMissing() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureInto(forwarded)).block();

        String forwardedId = forwarded.get().getRequest().getHeaders()
                .getFirst(CorrelationIdGlobalFilter.HEADER);
        assertThat(forwardedId).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER))
                .isEqualTo(forwardedId);
    }

    /** A well-formed client ID must be kept so a trace can start in the browser. */
    @Test
    void keepsValidIncomingId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/products")
                        .header(CorrelationIdGlobalFilter.HEADER, "demo-123").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureInto(forwarded)).block();

        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(CorrelationIdGlobalFilter.HEADER)).isEqualTo("demo-123");
    }

    /** An ID with unsafe characters must be replaced, never forwarded or logged. */
    @Test
    void replacesInvalidIncomingId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/products")
                        .header(CorrelationIdGlobalFilter.HEADER, "bad id\nINFO fake log line").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captureInto(forwarded)).block();

        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(CorrelationIdGlobalFilter.HEADER))
                .doesNotContain("\n")
                .doesNotContain(" ");
    }

    /** Builds a stub chain that records the exchange the filter passed on. */
    private GatewayFilterChain captureInto(AtomicReference<ServerWebExchange> target) {
        return exchange -> {
            target.set(exchange);
            return Mono.empty();
        };
    }

}
