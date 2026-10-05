package com.shopsphere.product.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that makes the correlation ID available to logging.
 *
 * <p>It reads the X-Correlation-Id header set by the gateway and stores it in the
 * MDC (a per-thread map used by the logger), so the log pattern can print it on
 * every line written while handling this request. If the service is called
 * directly (without the gateway), it creates an ID itself.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Header carrying the ID, same name the gateway uses. */
    private static final String HEADER = "X-Correlation-Id";

    /** Key under which the ID is stored in the MDC; the log pattern reads this key. */
    private static final String MDC_KEY = "correlationId";

    /**
     * Stores the correlation ID in the MDC for the duration of the request and
     * always clears it afterwards, because servlet threads are reused.
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response
     * @param chain    the remaining filters and the target servlet
     * @throws ServletException if the chain fails
     * @throws IOException      if writing the response fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);   // never leak the ID into the next request on this thread
        }
    }
}
