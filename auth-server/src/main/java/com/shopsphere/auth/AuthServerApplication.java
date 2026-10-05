package com.shopsphere.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the Authorization Server.
 *
 * <p>This service is the identity provider of ShopSphere: it logs users in and
 * issues the tokens (JWTs) that the other services trust. It does not contain any
 * business logic such as products or orders.
 */

@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthServerApplication {

    /**
     * Starts the Authorization Server.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }

}
