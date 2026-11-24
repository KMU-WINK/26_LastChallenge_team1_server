package com.wink.ichibang.post.exception;

import java.util.List;

/**
 * 오류 응답 본문.
 * 형태는 WINK-API공통규칙.md 6절을 받으면 그에 맞춰 이 클래스만 고친다.
 */
public record ErrorResponse(
        String errorCode,
        String message,
        List<FieldErrorResponse> fieldErrors
) {
    public static ErrorResponse of(PostErrorCode code) {
        return new ErrorResponse(code.name(), code.getMessage(), List.of());
    }

    public static ErrorResponse of(PostErrorCode code, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(code.name(), code.getMessage(), fieldErrors);
    }
}
