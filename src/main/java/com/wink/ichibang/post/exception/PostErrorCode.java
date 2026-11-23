package com.wink.ichibang.post.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 신청글 에러 코드 */
@Getter
@RequiredArgsConstructor
public enum PostErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "마감 일시는 시작 일시보다 늦어야 합니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "신청글을 찾을 수 없습니다."),
    POST_NOT_DRAFT(HttpStatus.CONFLICT, "임시 저장 상태인 신청글만 게시할 수 있습니다."),
    POST_PERIOD_PASSED(HttpStatus.CONFLICT, "마감 일시가 지나 게시할 수 없습니다."),
    POST_NOT_PUBLISHED(HttpStatus.CONFLICT, "게시되지 않은 신청글입니다."),
    POST_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 마감된 신청글입니다."),
    POST_FIELD_LOCKED(HttpStatus.CONFLICT, "현재 상태에서는 수정할 수 없는 항목입니다."),
    POST_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "신청자가 있는 신청글입니다. 확인 후 다시 요청하세요.");

    private final HttpStatus status;
    private final String message;
}
