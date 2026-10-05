package com.shopsphere.product.controller;

import com.shopsphere.product.dto.PageResponse;
import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.model.Product;
import com.shopsphere.product.security.RequiresShopAdmin;
import com.shopsphere.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST API for the product catalog. Reads are public; writes are restricted to
 * administrators through {@link RequiresShopAdmin}.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Product catalog APIs")
public class ProductController {

    /** Business logic for products. */
    private final ProductService service;

    /**
     * Creates a product (admin only).
     *
     * @param request the validated product data
     * @return 201 Created with a Location header and the created product
     */
    @PostMapping
    @RequiresShopAdmin
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Returns one product (public).
     *
     * @param id the product id
     * @return the product, or 404 if it does not exist
     */
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    /**
     * Lists products page by page, optionally filtered by category (public).
     *
     * @param category optional category filter
     * @param page     zero-based page number
     * @param size     page size, 1 to 100
     * @return one page of products
     */
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.list(category, page, size);
    }

    /**
     * Replaces a product's data (admin only).
     *
     * @param id      the product id
     * @param request the new data
     * @return the updated product
     */
    @PutMapping("/{id}")
    @RequiresShopAdmin
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    /**
     * Deletes a product (admin only).
     *
     * @param id the product id
     */
    @DeleteMapping("/{id}")
    @RequiresShopAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}