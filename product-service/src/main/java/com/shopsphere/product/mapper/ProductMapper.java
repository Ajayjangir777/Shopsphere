package com.shopsphere.product.mapper;

import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.model.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toProductResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getCategory(), p.getStock(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
