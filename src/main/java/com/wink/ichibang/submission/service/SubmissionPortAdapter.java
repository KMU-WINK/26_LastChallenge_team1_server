package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.port.SubmissionPort;
import com.wink.ichibang.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 신청글 도메인(post)이 제출 도메인에 묻는 창구의 실제 구현.
 * post.port.stub.StubSubmissionPort를 대신한다 (둘 다 있으면 빈 충돌로 앱이 안 뜨므로 stub은 삭제).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionPortAdapter implements SubmissionPort {

    private final SubmissionRepository submissionRepository;

    @Override
    public long countByPostId(Long postId) {
        return submissionRepository.countByPostId(postId);
    }

    @Override
    public Map<Long, Long> countByPostIds(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        return submissionRepository.countGroupedByPostId(postIds).stream()
                .collect(Collectors.toMap(
                        SubmissionRepository.PostCount::postId,
                        SubmissionRepository.PostCount::count));
    }

    @Override
    public Optional<MySubmission> findMySubmission(Long postId, Long memberId) {
        return submissionRepository.findByPostIdAndMemberId(postId, memberId)
                .map(s -> new MySubmission(s.getId(), s.getStatus().name(), s.getSubmittedAt()));
    }
}
