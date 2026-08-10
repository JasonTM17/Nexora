package com.nexora.platform.auth;

import org.springframework.http.HttpStatus;

public final class DomainAccessException extends RuntimeException {
    private final String code;
    private final String internalCode;
    private final HttpStatus status;

    public DomainAccessException(HttpStatus status, String code, String message) {
        this(status, code, code, message);
    }

    public DomainAccessException(HttpStatus status, String code, String internalCode, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.internalCode = internalCode;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String internalCode() {
        return internalCode;
    }
}
