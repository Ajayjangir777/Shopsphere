package com.shopsphere.gateway.config;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Configuration for rate limiting at the gateway.
 *
 * <p>The rate limiter keeps one "token bucket" per key in Redis. This class
 * defines how a request is mapped to its key, i.e. WHO is being limited.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Identifies a caller by their IP address, so each IP gets its own bucket.
     * Referenced from the route config as {@code #{@ipKeyResolver}}.
     *
     * <p>Limitation: behind a load balancer (like AWS ALB) the remote address is
     * the balancer's, not the user's. In production we would read X-Forwarded-For.
     * Once authentication exists we will switch to a per-user key.
     *
     * @return a resolver that maps a request to the caller's IP, or "unknown"
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress())
                        .orElse("unknown"));
    }

    /**
     * Identifies a caller for the sign-up route. The "register:" prefix gives it its own
     * bucket in Redis, separate from the product browsing bucket for the same IP, so
     * browsing the catalog can never use up someone's sign-up allowance (or the reverse).
     *
     * @return a resolver that maps a request to "register:" plus the caller's IP
     */
    @Bean
    public KeyResolver registerKeyResolver() {
        return exchange -> Mono.just("register:" + Optional
                .ofNullable(exchange.getRequest().getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("unknown"));
    }

}
