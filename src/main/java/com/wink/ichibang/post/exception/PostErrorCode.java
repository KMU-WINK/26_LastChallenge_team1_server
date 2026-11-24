package com.wink.ichibang.post.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 신청글·신청서 에러 코드 (기능 명세 POST-01~06, FORM-01) */
@Getter
@RequiredArgsConstructor
public enum PostErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "신청 기간을 확인해 주세요."),
    FORM_EMPTY(HttpStatus.BAD_REQUEST, "질문을 하나 이상 추가해 주세요."),
    INVALID_OPTIONS(HttpStatus.BAD_REQUEST, "선택지를 확인해 주세요. 객관식은 서로 다른 선택지 2개 이상, 단답형은 선택지 없음."),

    // 404
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "신청글을 찾을 수 없습니다."),

    // 409
    POST_CLOSED(HttpStatus.CONFLICT, "마감된 신청글은 수정할 수 없습니다."),
    POST_FIELD_LOCKED(HttpStatus.CONFLICT, "현재 상태에서는 수정할 수 없는 항목입니다."),
    FORM_LOCKED(HttpStatus.CONFLICT, "제출자가 있어 질문을 바꿀 수 없습니다."),
    POST_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 마감된 신청글입니다."),
    POST_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "제출자가 있는 신청글은 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
