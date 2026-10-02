package com.shopsphere.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(@NotBlank(message = "Name is required")
                             @Size(max = 150, message = "Name must be at most 150 characters")
                             String name,

                             @Size(max = 1000, message = "Description must be at most 1000 characters")
                             String description,

                             @NotNull(message = "Price is required")
                             @DecimalMin(value = "0.01", message = "Price must be greater than zero")
                             @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimal places")
                             BigDecimal price,

                             @NotBlank(message = "Category is required")
                             String category,

                             @NotNull(message = "Stock is required")
                             @PositiveOrZero(message = "Stock cannot be negative")
                             Integer stock) {


}
