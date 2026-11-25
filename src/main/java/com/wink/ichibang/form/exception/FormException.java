package com.wink.ichibang.form.exception;

import lombok.Getter;

@Getter
public class FormException extends RuntimeException {

    private final FormErrorCode errorCode;

    public FormException(FormErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
