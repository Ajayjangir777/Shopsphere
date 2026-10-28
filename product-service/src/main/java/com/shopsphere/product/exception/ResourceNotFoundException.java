package com.shopsphere.product.exception;

import com.shopsphere.common.web.ApiException;
import org.springframework.http.HttpStatus;

/** A product (or other resource) that does not exist; becomes 404 through {@link ApiException}. **/
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, resource + " not found with id: " + id);
    }
}
