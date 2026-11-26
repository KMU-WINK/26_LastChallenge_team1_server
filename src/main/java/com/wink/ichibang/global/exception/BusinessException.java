package com.wink.ichibang.global.exception;

import lombok.Getter;

import java.util.List;

/**
 * 모든 도메인이 던지는 비즈니스 예외. 컨트롤러에서 try-catch 하지 말고 이걸 던진다.
 * {@link GlobalExceptionHandler}가 { errorCode, message, fieldErrors } 응답으로 바꾼다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorResponse> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public BusinessException(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    /** 칸 하나가 잘못된 오류. 예: field(VALIDATION_FAILED, "type", "구분은 바꿀 수 없어요") */
    public static BusinessException field(ErrorCode errorCode, String field, String reason) {
        return new BusinessException(errorCode, List.of(new FieldErrorResponse(field, reason)));
    }
}
