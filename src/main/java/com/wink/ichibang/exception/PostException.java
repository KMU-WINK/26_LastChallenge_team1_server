package com.wink.ichibang.exception;

import lombok.Getter;

/** 신청글 도메인에서 던지는 예외. PostExceptionHandler가 에러 응답으로 바꾼다 */
@Getter
public class PostException extends RuntimeException {

    private final PostErrorCode errorCode;
    private final Object detail;   // 에러 응답의 detail (예: { "submissionCount": 12 })

    public PostException(PostErrorCode errorCode) {
        this(errorCode, null);
    }

    public PostException(PostErrorCode errorCode, Object detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
