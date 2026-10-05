package com.shopsphere.product.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as writable only by administrators.
 *
 * <p>Two conditions must both hold: the user has the ADMIN role (WHO the user is) and
 * the token carries the shop.write scope (WHAT the client app was allowed to do on the
 * user's behalf). Defining the rule once as an annotation keeps it consistent and
 * avoids repeating the expression on every method.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_shop.write')")
public @interface RequiresShopAdmin {

}
