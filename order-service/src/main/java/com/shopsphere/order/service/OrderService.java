package com.shopsphere.order.service;

import com.shopsphere.common.web.ApiException;
import com.shopsphere.order.CustomerOrder;
import com.shopsphere.order.Dto.CreateOrderRequest;
import com.shopsphere.order.Dto.OrderResponse;
import com.shopsphere.order.Dto.OrderSummaryResponse;
import com.shopsphere.order.Dto.PageResponse;
import com.shopsphere.order.OrderItem;
import com.shopsphere.order.OrderStatus;
import com.shopsphere.order.ShippingAddress;
import com.shopsphere.order.feignClient.AddressDto;
import com.shopsphere.order.feignClient.ProductClient;
import com.shopsphere.order.feignClient.ProductDto;
import com.shopsphere.order.feignClient.UserClient;
import com.shopsphere.order.integration.AddressLookup;
import com.shopsphere.order.integration.ProductLookup;
import com.shopsphere.order.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Order business logic. Prices, names and stock come from the Product Service and the
 * address from the User Service; the client only chooses WHAT to buy and WHERE to send it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    // Fields: replace ProductClient / UserClient with the resilient wrappers
    private final OrderRepository orderRepository;
    private final ProductLookup productLookup;
    private final AddressLookup addressLookup;
    /**
     * Places an order for the caller.
     *
     * <p>This method is deliberately NOT @Transactional: it makes network calls, and a
     * database connection must never be held open while waiting for other services. The
     * only transaction is the short one inside {@code save}.
     *
     * @param ownerSub the caller's token subject
     * @param request  address id and requested items
     * @return the created order
     */
    public OrderResponse create(String ownerSub, CreateOrderRequest request) {
        rejectDuplicateProducts(request);
        AddressDto address = addressLookup.getMyAddress(request.addressId());

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.Item requested : request.items()) {
            ProductDto product = productLookup.getById(requested.productId());
            // Advisory only: another order may take the stock a moment later.
            // Real reservation comes with the Saga step.
            if (product.stock() < requested.quantity()) {
                throw ApiException.conflict("Insufficient stock for product " + product.name());
            }
            items.add(OrderItem.builder().productId(product.id()).productName(product.name())
                    .unitPrice(product.price()).quantity(requested.quantity()).build());
            total = total.add(product.price().multiply(BigDecimal.valueOf(requested.quantity())));
        }

        CustomerOrder order = CustomerOrder.builder()
                .ownerSub(ownerSub)
                .status(OrderStatus.CREATED)
                .totalAmount(total)
                .shippingAddress(snapshot(address))
                .build();
        items.forEach(order::addItem);

        return OrderResponse.from(orderRepository.save(order));
    }

    /** Lists the caller's orders, newest first, without their items. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(String ownerSub, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(orderRepository.findByOwnerSub(ownerSub, pageable).map(OrderSummaryResponse::from));
    }

    /** Returns one of the caller's orders; someone else's order id gives 404, never 403. */
    @Transactional(readOnly = true)
    public OrderResponse get(String ownerSub, UUID id) {
        return OrderResponse.from(find(ownerSub, id));
    }

    /** Cancels the caller's order, allowed only while it is still CREATED. */
    @Transactional
    public OrderResponse cancel(String ownerSub, UUID id) {
        CustomerOrder order = find(ownerSub, id);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw ApiException.conflict("An order in status " + order.getStatus() + " cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);   // dirty checking writes it; @Version guards concurrent changes
        return OrderResponse.from(order);
    }

    /** Loads the caller's order with items or fails with 404. */
    private CustomerOrder find(String ownerSub, UUID id) {
        return orderRepository.findByIdAndOwnerSub(id, ownerSub)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + id));
    }

    /** The same product twice is almost certainly a client bug; ask the client to merge the lines. */
    private void rejectDuplicateProducts(CreateOrderRequest request) {
        Set<UUID> seen = new HashSet<>();
        for (CreateOrderRequest.Item item : request.items()) {
            if (!seen.add(item.productId())) {
                throw ApiException.badRequest("Product listed more than once: " + item.productId());
            }
        }
    }


    /** Copies the address into the order so later address edits never change old orders. */
    private ShippingAddress snapshot(AddressDto a) {
        return ShippingAddress.builder().recipientName(a.recipientName()).line1(a.line1()).line2(a.line2())
                .city(a.city()).state(a.state()).postalCode(a.postalCode()).country(a.country())
                .phone(a.phone()).build();
    }
}