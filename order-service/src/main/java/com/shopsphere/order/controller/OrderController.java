package com.shopsphere.order.controller;

import com.shopsphere.order.Dto.CreateOrderRequest;
import com.shopsphere.order.Dto.OrderResponse;
import com.shopsphere.order.Dto.OrderSummaryResponse;
import com.shopsphere.order.Dto.PageResponse;
import com.shopsphere.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** REST API for the caller's orders. The owner always comes from the verified token. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    /** Places an order; 201 with a Location header. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.create(jwt.getSubject(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Lists the caller's orders, paged. */
    @GetMapping
    public PageResponse<OrderSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                                   @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return orderService.list(jwt.getSubject(), page, size);
    }

    /** Returns one order with its items. */
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return orderService.get(jwt.getSubject(), id);
    }

    /** Cancels an order that has not progressed past CREATED. */
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return orderService.cancel(jwt.getSubject(), id);
    }
}
