package com.shopsphere.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/** One purchased product line. Name and price are snapshots, not live references. */
@Entity
@Table(name = "order_items")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owning order; set through {@link CustomerOrder#addItem}. */
    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    /** Id of the product in the Product Service (no database-level link across services). */
    @Column(name = "product_id", nullable = false) private UUID productId;

    /** Product name at the time of purchase. */
    @Column(name = "product_name", nullable = false, length = 150) private String productName;

    /** Price per unit at the time of purchase. */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;

    @Column(nullable = false) private int quantity;
}