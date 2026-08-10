package com.nexora.platform.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class TraceIdFilter implements Filter {

    public static final String ATTRIBUTE = TraceIdFilter.class.getName() + ".traceId";
    private static final String HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestedTraceId = httpRequest.getHeader(HEADER);
        String traceId = TraceIdPolicy.acceptedOrGenerated(requestedTraceId);

        request.setAttribute(ATTRIBUTE, traceId);
        httpResponse.setHeader(HEADER, traceId);
        chain.doFilter(request, response);
    }
}
