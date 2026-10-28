package com.shopsphere.order;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An order placed by one customer. Named CustomerOrder because ORDER is reserved in SQL
 * and JPQL. {@code @Version} guards against two requests changing it at the same time.
 */
@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** JWT subject of the customer who owns this order. */
    @Column(name = "owner_sub", nullable = false, length = 100)
    private String ownerSub;

    /** Current lifecycle state. */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Sum of unit price times quantity, computed by the server. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Address snapshot. */
    @Embedded
    private ShippingAddress shippingAddress;

    /** The purchased lines; saved and deleted together with the order. */
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /** Optimistic-lock counter, incremented on every update. */
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Adds a line and links it back to this order (keeps both sides of the relation in sync). */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}