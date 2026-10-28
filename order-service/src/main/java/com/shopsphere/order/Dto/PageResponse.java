package com.shopsphere.order.Dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Generic page wrapper, same shape as the Product Service's. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Converts a Spring Data page. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
