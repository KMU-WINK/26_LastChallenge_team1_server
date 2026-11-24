package com.wink.ichibang.post.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class PostException extends RuntimeException {

    private final PostErrorCode errorCode;
    private final List<FieldErrorResponse> fieldErrors;

    public PostException(PostErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public PostException(PostErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    /** 칸 하나가 잘못된 VALIDATION_FAILED */
    public static PostException field(String field, String reason) {
        return new PostException(PostErrorCode.VALIDATION_FAILED, List.of(new FieldErrorResponse(field, reason)));
    }
}
