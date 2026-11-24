package com.wink.ichibang.post.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 신청글 도메인 오류 코드. 코드와 문구는 명세 COM-04 표(= API 공통 규칙 6절)를 따른다.
 * UNAUTHORIZED / FORBIDDEN은 인증 필터(COM-01)가 내려주므로 여기 없다.
 */
@Getter
@RequiredArgsConstructor
public enum PostErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "필수 항목을 입력해주세요"),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "신청 기간을 확인해주세요"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "신청글을 찾을 수 없어요"),
    POST_CLOSED(HttpStatus.CONFLICT, "이미 마감된 신청글이에요"),
    POST_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "제출자가 있어서 처리할 수 없어요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해주세요");

    private final HttpStatus status;
    private final String message;
}
