package com.shopsphere.product.repository;


import com.shopsphere.product.config.JpaAuditingConfig;
import com.shopsphere.product.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
public class ProductRepositoryTest {


    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    ProductRepository repository;

    @Test
    void save_populatesIdAuditFieldsAndVersion() {
        Product saved = repository.saveAndFlush(product("iPhone 15", "mobiles"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void findByCategory_returnsOnlyMatchingProductsPaged() {
        repository.save(product("iPhone 15", "mobiles"));
        repository.save(product("Pixel 9", "mobiles"));
        repository.save(product("MacBook Air", "laptops"));

        Page<Product> page = repository.findByCategory("mobiles", PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    private Product product(String name, String category) {
        return Product.builder()
                .name(name)
                .description("test")
                .price(new BigDecimal("999.00"))
                .category(category)
                .stock(10)
                .build();
    }

}
