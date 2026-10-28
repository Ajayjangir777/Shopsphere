package com.shopsphere.order.Dto;

import com.shopsphere.order.CustomerOrder;
import com.shopsphere.order.OrderStatus;
import com.shopsphere.order.ShippingAddress;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, OrderStatus status, BigDecimal totalAmount, Address shippingAddress,
                            List<Item> items, Instant createdAt) {

    /** Converts an entity (items must already be loaded) to its response form. */
    public static OrderResponse from(CustomerOrder o) {
        ShippingAddress a = o.getShippingAddress();
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotalAmount(),
                new Address(a.getRecipientName(), a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                        a.getPostalCode(), a.getCountry(), a.getPhone()),
                o.getItems().stream().map(i -> new Item(i.getProductId(), i.getProductName(), i.getUnitPrice(),
                        i.getQuantity(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList(),
                o.getCreatedAt());
    }

}
