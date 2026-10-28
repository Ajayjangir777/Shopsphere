package com.shopsphere.common.web;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Puts the X-Correlation-Id header into the logging context (MDC) so every log line of
 * a request carries it. Without a valid header (e.g. direct calls) it creates an ID.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Header carrying the ID; the gateway and Feign clients use the same name. */
    public static final String HEADER = "X-Correlation-Id";

    /** MDC key read by the log pattern ({@code %X{correlationId}}). */
    public static final String MDC_KEY = "correlationId";

    /** Only short, simple IDs are accepted, which blocks log injection. */
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    /** Stores the ID in the MDC for this request and always clears it (threads are reused). */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || !VALID_ID.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }


}
