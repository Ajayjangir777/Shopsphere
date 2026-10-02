package com.shopsphere.configserver;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConfigServerTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void servesProductServiceConfig() {
        JsonNode env = rest.getForObject("/product-service/default", JsonNode.class);

        assertThat(env.get("name").asText()).isEqualTo("product-service");

        boolean hasPort = StreamSupport.stream(env.get("propertySources").spliterator(), false)
                .anyMatch(ps -> ps.get("source").has("server.port"));
        assertThat(hasPort).isTrue();
    }

    @Test
    void servesSharedConfigToEveryService() {
        JsonNode env = rest.getForObject("/any-other-service/default", JsonNode.class);

        boolean hasEureka = StreamSupport.stream(env.get("propertySources").spliterator(), false)
                .anyMatch(ps -> ps.get("source").has("eureka.client.service-url.defaultZone"));
        assertThat(hasEureka).isTrue();
    }

}
