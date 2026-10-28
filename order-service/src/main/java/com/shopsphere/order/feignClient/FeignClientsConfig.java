package com.shopsphere.order.feignClient;

import com.shopsphere.common.web.CorrelationIdFilter;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Feign clients and registers settings that apply to ALL of them. It lives in its
 * own @Configuration class (not on the main class) so slice tests such as @DataJpaTest,
 * which skip @Configuration classes, don't try to build Feign clients.
 */
@Configuration
@EnableFeignClients(basePackageClasses = ProductClient.class)
public class FeignClientsConfig {

    /** Passes the correlation ID to every downstream call so one request can be followed across services. */
    @Bean
    public RequestInterceptor correlationIdRelayInterceptor() {
        return template -> {
            String id = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (id != null) {
                template.header(CorrelationIdFilter.HEADER, id);
            }
        };
    }
}
