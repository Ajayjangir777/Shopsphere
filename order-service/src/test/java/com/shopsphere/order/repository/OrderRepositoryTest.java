package com.shopsphere.order.repository;

import com.shopsphere.order.CustomerOrder;
import com.shopsphere.order.OrderItem;
import com.shopsphere.order.OrderStatus;
import com.shopsphere.order.ShippingAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies mapping, cascade, entity-graph loading and owner scoping against a real PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    OrderRepository repository;
    @Autowired
    TestEntityManager em;

    /** The owner gets the order with both items; anyone else gets nothing. */
    @Test
    void findByIdAndOwnerSub_loadsItemsForOwnerOnly() {
        CustomerOrder saved = repository.saveAndFlush(order("alice"));
        em.clear();   // force a real reload from the database

        Optional<CustomerOrder> mine = repository.findByIdAndOwnerSub(saved.getId(), "alice");

        assertThat(mine).isPresent();
        assertThat(mine.get().getItems()).hasSize(2);
        assertThat(repository.findByIdAndOwnerSub(saved.getId(), "mallory")).isEmpty();
    }

    /** Paging only returns the owner's orders. */
    @Test
    void findByOwnerSub_returnsOnlyOwnersOrders() {
        repository.save(order("alice"));
        repository.save(order("alice"));
        repository.save(order("bob"));

        Page<CustomerOrder> page = repository.findByOwnerSub("alice", PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    /** Builds an order with two items for the given owner. */
    private static CustomerOrder order(String owner) {
        CustomerOrder order = CustomerOrder.builder().ownerSub(owner).status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("30.00"))
                .shippingAddress(ShippingAddress.builder().recipientName("Test").line1("12 Main Road")
                        .city("Pune").state("MH").postalCode("411001").country("IN").build()).build();
        order.addItem(OrderItem.builder().productId(UUID.randomUUID()).productName("A")
                .unitPrice(new BigDecimal("10.00")).quantity(1).build());
        order.addItem(OrderItem.builder().productId(UUID.randomUUID()).productName("B")
                .unitPrice(new BigDecimal("10.00")).quantity(2).build());
        return order;
    }
}