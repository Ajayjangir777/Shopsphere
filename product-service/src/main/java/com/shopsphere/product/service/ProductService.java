package com.shopsphere.product.service;

import com.shopsphere.product.config.CacheConfig;
import com.shopsphere.product.dto.PageResponse;
import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.exception.ResourceNotFoundException;
import com.shopsphere.product.mapper.ProductMapper;
import com.shopsphere.product.model.Product;
import com.shopsphere.product.repository.ProductRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;


    /**
     *
     * @param request
     * This method is  used to add product
     * @return
     */
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .stock(request.stock())
                .build();
        return ProductMapper.toProductResponse(productRepository.save(product));
    }


    @Cacheable(cacheNames = CacheConfig.PRODUCT_CACHE, key = "#id.toString()", sync = true)
    public ProductResponse getById(UUID id) {
        return ProductMapper.toProductResponse(findOrThrow(id));
    }

    public PageResponse<ProductResponse> list(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Product> result = (category == null)
                ? productRepository.findAll(pageable)
                : productRepository.findByCategory(category, pageable);

        return PageResponse.from(result.map(ProductMapper::toProductResponse));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_CACHE, key = "#id.toString()")
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findOrThrow(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(request.category());
        product.setStock(request.stock());
        // no save() needed: Hibernate's dirty checking flushes the changes at commit
        return ProductMapper.toProductResponse(product);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_CACHE, key = "#id.toString()")
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }

    private Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

}
