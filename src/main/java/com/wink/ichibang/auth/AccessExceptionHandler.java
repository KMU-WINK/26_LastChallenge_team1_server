package com.wink.ichibang.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccessExceptionHandler {

    @ExceptionHandler(AccessException.class)
    public ResponseEntity<ErrorBody> handle(AccessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorBody(
                        false,
                        null,
                        e.getCode(),
                        e.getMessage()
                ));
    }

    private record ErrorBody(
            boolean success,
            Object data,
            String errorCode,
            String message
    ) {
    }
}