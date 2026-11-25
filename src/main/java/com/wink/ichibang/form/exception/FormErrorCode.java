package com.wink.ichibang.form.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 신청서·제출자 관리(FORM-02~06) 오류 코드. 코드와 문구는 명세 COM-04 표를 따른다 */
@Getter
@RequiredArgsConstructor
public enum FormErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "필수 항목을 입력해주세요"),
    APPLY_POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "모집이 마감되었어요"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "신청글을 찾을 수 없어요"),
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청 내역이 없어요"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "이미 처리된 상태예요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해주세요");

    private final HttpStatus status;
    private final String message;
}
