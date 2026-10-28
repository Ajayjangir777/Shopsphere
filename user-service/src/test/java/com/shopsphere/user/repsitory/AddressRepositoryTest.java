package com.shopsphere.user.repsitory;


import com.shopsphere.user.Address;
import com.shopsphere.user.AddressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the ownership rules against a real PostgreSQL (Flyway runs the real migration too). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class AddressRepositoryTest {


    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    AddressRepository repository;

    /** An address is visible to its owner and invisible to everyone else. */
    @Test
    void findByIdAndOwnerSub_returnsAddressOnlyToItsOwner() {
        Address saved = repository.saveAndFlush(address("alice"));

        assertThat(repository.findByIdAndOwnerSub(saved.getId(), "alice")).isPresent();
        assertThat(repository.findByIdAndOwnerSub(saved.getId(), "mallory")).isEmpty();
    }

    /** Deleting with the wrong owner removes nothing. */
    @Test
    void deleteByIdAndOwnerSub_cannotDeleteSomeoneElsesAddress() {
        Address saved = repository.saveAndFlush(address("alice"));

        assertThat(repository.deleteByIdAndOwnerSub(saved.getId(), "mallory")).isZero();
        assertThat(repository.existsById(saved.getId())).isTrue();
        assertThat(repository.deleteByIdAndOwnerSub(saved.getId(), "alice")).isEqualTo(1);
    }

    /** The per-user count ignores other users' rows. */
    @Test
    void countByOwnerSub_countsOnlyOwnersAddresses() {
        repository.save(address("alice"));
        repository.save(address("alice"));
        repository.save(address("bob"));

        assertThat(repository.countByOwnerSub("alice")).isEqualTo(2);
    }

    /** Builds a valid address for the given owner. */
    private static Address address(String owner) {
        return Address.builder().ownerSub(owner).label("Home").recipientName("Test User")
                .line1("12 Main Road").city("Pune").state("Maharashtra")
                .postalCode("411001").country("IN").build();
    }
}
