package com.shopsphere.order.service;

import com.shopsphere.common.web.ApiException;
import com.shopsphere.order.CustomerOrder;
import com.shopsphere.order.OrderItem;
import com.shopsphere.order.OrderStatus;
import com.shopsphere.order.ShippingAddress;
import com.shopsphere.order.Dto.CreateOrderRequest;
import com.shopsphere.order.Dto.OrderResponse;
import com.shopsphere.order.feignClient.AddressDto;
import com.shopsphere.order.feignClient.ProductClient;
import com.shopsphere.order.feignClient.ProductDto;
import com.shopsphere.order.feignClient.UserClient;
import com.shopsphere.order.integration.AddressLookup;
import com.shopsphere.order.integration.ProductLookup;
import com.shopsphere.order.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Business-rule tests with mocked Feign clients and repository: fast, no Spring, no network. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    ProductLookup productLookup;
    @Mock
    AddressLookup addressLookup;
    @InjectMocks
    OrderService service;

    private final UUID addressId = UUID.randomUUID();
    private final UUID phoneId = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();

    /** Totals come from the Product Service's prices, and the address is copied into the order. */
    @Test
    void create_usesServerSidePricesAndSnapshotsAddress() {
        when(addressLookup.getMyAddress(addressId)).thenReturn(address());
        when(productLookup.getById(phoneId)).thenReturn(new ProductDto(phoneId, "Phone", new BigDecimal("100.00"), 10));
        when(productLookup.getById(caseId)).thenReturn(new ProductDto(caseId, "Case", new BigDecimal("50.50"), 10));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.create("alice", request(item(phoneId, 2), item(caseId, 1)));

        assertThat(response.totalAmount()).isEqualByComparingTo("250.50");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.shippingAddress().city()).isEqualTo("Pune");
        assertThat(response.items()).hasSize(2);

        ArgumentCaptor<CustomerOrder> saved = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerSub()).isEqualTo("alice");
    }


    /** Asking for more than the stock is a conflict (409). */
    @Test
    void create_rejectsInsufficientStock() {
        when(addressLookup.getMyAddress(addressId)).thenReturn(address());
        when(productLookup.getById(phoneId)).thenReturn(new ProductDto(phoneId, "Phone", new BigDecimal("100.00"), 1));

        assertStatus(() -> service.create("alice", request(item(phoneId, 2))), HttpStatus.CONFLICT);
    }


    /** The same product twice is rejected before any remote call is made. */
    @Test
    void create_rejectsDuplicateProducts() {
        assertStatus(() -> service.create("alice", request(item(phoneId, 1), item(phoneId, 2))), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(addressLookup, productLookup);
    }

    /** Another user's order id looks exactly like a missing one. */
    @Test
    void get_otherUsersOrder_returns404() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndOwnerSub(id, "mallory")).thenReturn(Optional.empty());

        assertStatus(() -> service.get("mallory", id), HttpStatus.NOT_FOUND);
    }

    /** A CREATED order can be cancelled. */
    @Test
    void cancel_changesStatusToCancelled() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndOwnerSub(id, "alice")).thenReturn(Optional.of(order(OrderStatus.CREATED)));

        assertThat(service.cancel("alice", id).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    /** An already-cancelled order cannot be cancelled again. */
    @Test
    void cancel_failsWhenNotCreated() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndOwnerSub(id, "alice")).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));

        assertStatus(() -> service.cancel("alice", id), HttpStatus.CONFLICT);
    }

    /** If the product lookup fails (e.g. circuit open), the error is passed on and nothing is saved. */
    @Test
    void create_whenProductLookupFails_savesNothing() {
        when(addressLookup.getMyAddress(addressId)).thenReturn(address());
        when(productLookup.getById(phoneId)).thenThrow(ApiException.unavailable("Product service is temporarily unavailable"));

        assertStatus(() -> service.create("alice", request(item(phoneId, 1))), HttpStatus.SERVICE_UNAVAILABLE);
        verify(orderRepository, never()).save(any());
    }

    /** If the address is invalid, products are never even looked up. */
    @Test
    void create_whenAddressLookupFails_skipsProductCalls() {
        when(addressLookup.getMyAddress(addressId)).thenThrow(ApiException.unprocessable("Address not found"));

        assertStatus(() -> service.create("alice", request(item(phoneId, 1))), HttpStatus.UNPROCESSABLE_ENTITY);
        verifyNoInteractions(productLookup);
    }

    /** Asserts that the call throws an ApiException with the expected status. */
    private static void assertStatus(ThrowableAssert.ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(expected));
    }

    private CreateOrderRequest request(CreateOrderRequest.Item... items) {
        return new CreateOrderRequest(addressId, List.of(items));
    }

    private static CreateOrderRequest.Item item(UUID productId, int quantity) {
        return new CreateOrderRequest.Item(productId, quantity);
    }


    private static AddressDto address() {
        return new AddressDto(UUID.randomUUID(), "Test User", "12 Main Road", null, "Pune", "Maharashtra",
                "411001", "IN", null);
    }

    private static CustomerOrder order(OrderStatus status) {
        CustomerOrder order = CustomerOrder.builder().ownerSub("alice").status(status)
                .totalAmount(new BigDecimal("10.00"))
                .shippingAddress(ShippingAddress.builder().recipientName("Test").line1("x").city("Pune")
                        .state("MH").postalCode("411001").country("IN").build()).build();
        order.addItem(OrderItem.builder().productId(UUID.randomUUID()).productName("Phone")
                .unitPrice(new BigDecimal("10.00")).quantity(1).build());
        return order;
    }
}