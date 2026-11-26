package com.wink.ichibang.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 팀 공통 오류 코드. 코드·상태·문구는 명세 COM-04 표(= API 공통 규칙 6절)를 따른다.
 * 도메인마다 enum을 따로 두면 같은 코드가 복사되어 문구가 어긋나므로 한 곳에서 관리한다.
 * 새 코드는 해당 도메인 구역에 추가한다 (여러 명이 같은 파일을 고치므로 구역을 지켜 충돌을 줄인다).
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ────────────────────────────────────────────────
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "필수 항목을 입력해주세요"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청을 확인해주세요"),
    PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없어요"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식이에요"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식이에요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해주세요"),

    // ── 인증·권한 (auth) ─────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요, 다시 로그인해주세요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없어요"),

    // ── 신청글 (post) ────────────────────────────────────────
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "신청 기간을 확인해주세요"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "신청글을 찾을 수 없어요"),
    POST_CLOSED(HttpStatus.CONFLICT, "이미 마감된 신청글이에요"),
    POST_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "제출자가 있어서 처리할 수 없어요"),

    // ── 제출 (submission) ────────────────────────────────────
    INVALID_ANSWER(HttpStatus.BAD_REQUEST, "답변을 확인해주세요"),
    APPLY_POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "모집이 마감되었어요"),
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청 내역이 없어요"),
    DUPLICATE_SUBMISSION(HttpStatus.CONFLICT, "이미 제출한 신청글이에요"),
    SUBMISSION_COMPLETED(HttpStatus.CONFLICT, "참여 완료된 신청은 수정·취소할 수 없어요"),

    // ── 신청서·제출자 관리 (form) ────────────────────────────
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "이미 처리된 상태예요");

    private final HttpStatus status;
    private final String message;
}
