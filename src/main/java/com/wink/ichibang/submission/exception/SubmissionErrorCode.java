package com.wink.ichibang.submission.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 제출 도메인에서 내보내는 오류 코드. 코드와 문구는 명세 COM-04 표(= API 공통 규칙 6절)를 따른다.
 * post 쪽 PostErrorCode와 겹치는 코드(POST_NOT_FOUND, VALIDATION_FAILED, INTERNAL_ERROR)는
 * 같은 값을 쓴다. 팀 공통 오류 처리(global)가 생기면 그쪽으로 합친다.
 * UNAUTHORIZED는 인증 필터(COM-01)가 생기면 필터가 내려주므로 이 항목을 지운다.
 */
@Getter
@RequiredArgsConstructor
public enum SubmissionErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요, 다시 로그인해주세요"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "필수 항목을 입력해주세요"),
    INVALID_ANSWER(HttpStatus.BAD_REQUEST, "답변을 확인해주세요"),
    APPLY_POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "모집이 마감되었어요"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "신청글을 찾을 수 없어요"),
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청 내역이 없어요"),
    DUPLICATE_SUBMISSION(HttpStatus.CONFLICT, "이미 제출한 신청글이에요"),
    SUBMISSION_COMPLETED(HttpStatus.CONFLICT, "참여 완료된 신청은 수정·취소할 수 없어요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해주세요");

    private final HttpStatus status;
    private final String message;
}
