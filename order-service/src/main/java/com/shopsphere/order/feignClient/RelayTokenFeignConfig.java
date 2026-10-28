package com.shopsphere.order.feignClient;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration used ONLY by {@link UserClient}. It is deliberately NOT annotated
 * with @Configuration: such a class would be picked up by component scanning and apply
 * to every Feign client, sending the user's token to services that never need it.
 */
public class RelayTokenFeignConfig {

    /** Adds the token-relay interceptor to this client. */
    @Bean
    public RequestInterceptor tokenRelayInterceptor() {
        return new TokenRelayInterceptor();
    }
}
