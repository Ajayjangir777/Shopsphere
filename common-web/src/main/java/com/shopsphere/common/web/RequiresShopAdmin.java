package com.shopsphere.common.web;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Endpoint is writable only by administrators: the ADMIN role (who the user is) AND the
 * shop.write scope (what the client app was allowed to do on the user's behalf).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_shop.write')")
public @interface RequiresShopAdmin {


}
