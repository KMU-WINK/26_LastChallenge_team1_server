package com.wink.ichibang.form.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.wink.ichibang.form")
public class FormExceptionHandler {

    @ExceptionHandler(FormException.class)
    public ResponseEntity<ErrorBody> form(FormException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ErrorBody(
                        false,
                        null,
                        exception.getCode(),
                        exception.getMessage()
                ));
    }

    public record ErrorBody(
            boolean success,
            Object data,
            String errorCode,
            String message
    ) {
    }
}