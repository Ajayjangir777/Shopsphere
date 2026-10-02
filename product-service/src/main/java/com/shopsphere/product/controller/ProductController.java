package com.shopsphere.product.controller;

import com.shopsphere.product.dto.PageResponse;
import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.model.Product;
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

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Product catalog APIs")
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;


    @PostMapping
   public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request) {
       ProductResponse created = productService.create(request);
       URI location = ServletUriComponentsBuilder.fromCurrentRequest()
               .path("/{id}").buildAndExpand(created.id()).toUri();
       return ResponseEntity.created(location).body(created);

   }

   @GetMapping("/{id}")
   public ProductResponse getProductById(@PathVariable UUID id) {
        return  productService.getById(id);
   }

   @GetMapping
    public PageResponse<ProductResponse> productList(@RequestParam(required = false) String category,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

       return productService.list(category, page, size);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
