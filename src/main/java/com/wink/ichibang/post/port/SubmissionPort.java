package com.wink.ichibang.post.port;

/**
 * 신청글 도메인이 신청서 제출(다른 담당)에 요청하는 기능.
 * 제출 담당의 서비스가 이 인터페이스를 구현하면 된다.
 * 구현이 생기기 전까지는 stub/StubSubmissionPort가 대신한다.
 */
public interface SubmissionPort {

    /**
     * 신청글의 제출자 수.
     * 목록의 제출 인원, 질문 수정 잠금(FORM_LOCKED), 삭제 대신 마감(POST-03)에 쓴다.
     */
    long countSubmissions(Long postId);
}
