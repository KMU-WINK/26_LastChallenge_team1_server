package com.wink.ichibang.form.exception;

import org.springframework.http.HttpStatus;

public class FormException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public FormException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}