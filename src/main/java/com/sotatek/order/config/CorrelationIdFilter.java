package com.sotatek.order.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(MDC_TRACE_ID_KEY, traceId);

        if (response instanceof HttpServletResponse) {
            ((HttpServletResponse) response).addHeader(TRACE_ID_HEADER, traceId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }
}
