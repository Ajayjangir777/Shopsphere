package com.shopsphere.product.service;


import com.shopsphere.product.config.CacheConfig;
import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

@SpringBootTest
@Testcontainers
public class ProductCachingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    ProductService service;

    @Autowired
    CacheManager cacheManager;

    private Cache productCache() {
        return cacheManager.getCache(CacheConfig.PRODUCT_CACHE);
    }

    @Test
    void getById_populatesCacheOnFirstRead() {
        ProductResponse created = service.create(request("iPhone 15"));
        assertThat(productCache().get(created.id().toString())).isNull();

        service.getById(created.id());

        assertThat(productCache().get(created.id().toString())).isNotNull();
    }

    @Test
    void cachedValue_roundTripsThroughRedisAsJson() {
        ProductResponse created = service.create(request("Pixel 9"));

        ProductResponse fromDb = service.getById(created.id());      // miss → DB → cached
        ProductResponse fromCache = service.getById(created.id());   // hit → Redis → deserialized

        assertThat(fromCache).isEqualTo(fromDb);
    }

    @Test
    void update_evictsCacheAndNextReadSeesNewData() {
        ProductResponse created = service.create(request("Old name"));
        service.getById(created.id());
        assertThat(productCache().get(created.id().toString())).isNotNull();

        service.update(created.id(), request("New name"));

        assertThat(productCache().get(created.id().toString())).isNull();
        assertThat(service.getById(created.id()).name()).isEqualTo("New name");
    }

    @Test
    void delete_evictsCache() {
        ProductResponse created = service.create(request("To delete"));
        service.getById(created.id());

        service.delete(created.id());

        assertThat(productCache().get(created.id().toString())).isNull();
        assertThatThrownBy(() -> service.getById(created.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ProductRequest request(String name) {
        return new ProductRequest(name, "test", new BigDecimal("999.00"), "mobiles", 10);
    }
}
