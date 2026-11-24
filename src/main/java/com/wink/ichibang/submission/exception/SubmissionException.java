package com.wink.ichibang.submission.exception;

import com.wink.ichibang.post.exception.FieldErrorResponse;
import lombok.Getter;

import java.util.List;

/** 제출 도메인에서 던지는 예외. SubmissionExceptionHandler가 오류 응답으로 바꾼다 */
@Getter
public class SubmissionException extends RuntimeException {

    private final SubmissionErrorCode errorCode;
    private final List<FieldErrorResponse> fieldErrors;

    public SubmissionException(SubmissionErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public SubmissionException(SubmissionErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    /** 답변 한 칸이 잘못된 INVALID_ANSWER. field 예: answers[1].optionIds */
    public static SubmissionException invalidAnswer(String field, String reason) {
        return new SubmissionException(SubmissionErrorCode.INVALID_ANSWER,
                List.of(new FieldErrorResponse(field, reason)));
    }
}
