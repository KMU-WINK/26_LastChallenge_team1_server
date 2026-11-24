package com.wink.ichibang.post.port.stub;

import com.wink.ichibang.post.port.SubmissionPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 임시 구현: 제출이 하나도 없다고 답한다.
 * 제출 도메인이 SubmissionPort를 구현하면 이 파일을 삭제한다 (둘 다 있으면 빈 충돌로 앱이 안 뜬다).
 */
@Component
public class StubSubmissionPort implements SubmissionPort {

    @Override
    public long countByPostId(Long postId) {
        return 0;
    }

    @Override
    public Map<Long, Long> countByPostIds(Collection<Long> postIds) {
        return Map.of();
    }

    @Override
    public Optional<MySubmission> findMySubmission(Long postId, Long memberId) {
        return Optional.empty();
    }
}
