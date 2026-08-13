package com.nexora.platform.auth;

import com.nexora.platform.api.ApiProblem;
import com.nexora.platform.observability.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.nexora.platform.authorization", "com.nexora.platform.identity",
        "com.nexora.platform.tenant", "com.nexora.platform.profile", "com.nexora.platform.cms",
        "com.nexora.platform.realtime", "com.nexora.platform.events"
})
public class IdentityApiExceptionHandler implements AuthenticationEntryPoint {
    private static final String GENERIC_PERMISSION_DENIED_MESSAGE = "Permission denied.";

    @ExceptionHandler(DomainAccessException.class)
    ResponseEntity<ApiProblem> domainFailure(DomainAccessException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(problem(exception.code(), clientMessage(exception), request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiProblem> unreadableRequest(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(problem("validation_failed", "Request validation failed.", request));
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String traceId = (String) request.getAttribute(TraceIdFilter.ATTRIBUTE);
        response.getWriter().write("{\"code\":\"AUTHENTICATION_REQUIRED\","
                + "\"message\":\"A valid bearer token is required.\","
                + "\"details\":{},\"traceId\":\"" + traceId + "\"}");
    }

    private ApiProblem problem(String code, String message, HttpServletRequest request) {
        return new ApiProblem(code, message, Map.of(), (String) request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }

    private String clientMessage(DomainAccessException exception) {
        if ("PERMISSION_DENIED".equals(exception.code())) {
            return GENERIC_PERMISSION_DENIED_MESSAGE;
        }
        return exception.getMessage();
    }
}
