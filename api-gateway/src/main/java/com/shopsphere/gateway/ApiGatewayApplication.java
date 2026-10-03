package com.shopsphere.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the API Gateway.
 *
 * <p>The gateway is the single public door to the system: clients call this
 * service (port 8080) and it forwards each request to the right microservice,
 * located by name through Eureka. It also applies cross-cutting concerns
 * (CORS, rate limiting, correlation IDs) so individual services don't have to.
 */
@SpringBootApplication
public class ApiGatewayApplication {


    /**
     * Starts the gateway application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
