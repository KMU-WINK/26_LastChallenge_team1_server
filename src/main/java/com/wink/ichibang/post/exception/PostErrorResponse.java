package com.wink.ichibang.post.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 에러 응답 본문: { "code": "POST_NOT_FOUND", "message": "...", "detail": ... }
 * 팀 공통 응답 형식이 정해지면 그쪽으로 옮긴다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostErrorResponse(String code, String message, Object detail) {

    public static PostErrorResponse of(PostErrorCode errorCode, Object detail) {
        return new PostErrorResponse(errorCode.name(), errorCode.getMessage(), detail);
    }
}
