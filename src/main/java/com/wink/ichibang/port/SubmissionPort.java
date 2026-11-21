package com.wink.ichibang.port;

/**
 * 신청글 도메인이 제출처리(submission, BE-2)에 요청하는 기능.
 * BE-2의 SubmissionService가 이 인터페이스를 구현하면 된다.
 * 구현이 생기기 전까지는 stub/StubSubmissionPort가 대신한다.
 */
public interface SubmissionPort {

    /** 취소를 제외한 제출 수 (삭제 확인, 운영진 목록·상세의 submissionCount) */
    long countActiveSubmissions(Long postId);
}
