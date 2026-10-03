package com.shopsphere.gateway.filter;


import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Global filter that guarantees every request carries a correlation ID.
 *
 * <p>A correlation ID is a unique value attached to one client request and passed
 * along to every service it touches, so all log lines for that request can be
 * found with a single search. This filter reuses a valid ID sent by the client,
 * otherwise generates one, forwards it downstream, and echoes it in the response.
 * It also logs one summary line per request with status and duration.
 */
@Slf4j
@Component
public class CorrelationIdGlobalFilter  implements GlobalFilter, Ordered {

    /** Name of the HTTP header that carries the correlation ID. */
    public static final String HEADER = "X-Correlation-Id";

    /**
     * Only short, simple IDs are accepted from clients. This blocks log injection
     * (e.g. an ID containing newlines) and oversized header values.
     */
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    /**
     * Resolves the correlation ID, adds it to the forwarded request and the
     * response, and logs the outcome once the request completes.
     *
     * @param exchange the current request/response pair
     * @param chain    the rest of the filter chain
     * @return a Mono that completes when the request has been processed
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange.getRequest());

        // Requests are immutable, so we build a modified copy that carries the ID downstream.
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER, correlationId)
                .build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();

        // Let the caller see the ID too, which is useful when reporting a problem.
        mutated.getResponse().getHeaders().set(HEADER, correlationId);

        long startNanos = System.nanoTime();
        return chain.filter(mutated).doFinally(signal -> {
            long millis = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("[{}] {} {} -> {} in {} ms",
                    correlationId,
                    request.getMethod(),
                    request.getURI().getPath(),
                    mutated.getResponse().getStatusCode(),
                    millis);
        });
    }

    /**
     * Runs before every other filter, so even rate-limited (429) responses carry the ID.
     *
     * @return the highest precedence, i.e. first in the chain
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Returns the client's ID if it is valid, otherwise a new random UUID.
     *
     * @param request the incoming request
     * @return the correlation ID to use for this request
     */
    private String resolveCorrelationId(ServerHttpRequest request) {
        String incoming = request.getHeaders().getFirst(HEADER);
        if (incoming != null && VALID_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }

}
