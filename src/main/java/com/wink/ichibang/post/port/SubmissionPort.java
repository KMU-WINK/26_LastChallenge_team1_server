package com.wink.ichibang.post.port;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 신청글 도메인이 제출 도메인(김보민, SUB-01~05)에게 묻는 것만 모은 창구.
 * 제출 도메인이 이 인터페이스를 구현하면 stub/StubSubmissionPort를 지운다.
 * 취소한 제출은 행이 삭제되므로 남은 제출 = 제출됨(SUBMITTED) + 참여 완료(COMPLETED).
 */
public interface SubmissionPort {

    /** 글 하나의 제출 수 (POST-02 잠금, POST-03 삭제 가능 여부) */
    long countByPostId(Long postId);

    /** 여러 글의 제출 수. 제출이 없는 글은 Map에 없어도 된다 (POST-07 목록) */
    Map<Long, Long> countByPostIds(Collection<Long> postIds);

    /** 로그인한 부원의 이 글 제출 요약 (POST-05 mySubmission) */
    Optional<MySubmission> findMySubmission(Long postId, Long memberId);

    /** @param status SUBMITTED / COMPLETED */
    record MySubmission(Long submissionId, String status, LocalDateTime submittedAt) {
    }
}
