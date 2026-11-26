package com.wink.ichibang.submission.service;

import com.wink.ichibang.submission.domain.Submission;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import com.wink.ichibang.submission.dto.SubmissionView;
import com.wink.ichibang.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 다른 도메인(form)이 제출을 읽는 창구 (FORM-02, 03, 05, 06).
 * 제출·답변 테이블은 submission 도메인 것이라 form은 SQL로 직접 읽지 않고 이 서비스를 쓴다.
 * 목록은 모두 제출 일시 최신순(같으면 ID 큰 순)이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionQueryService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"));

    private final SubmissionRepository submissionRepository;

    /** 이 부원이 이 글에 낸 제출 (답변 포함). 없으면 비어 있다 */
    public Optional<SubmissionView> findMine(Long postId, Long memberId) {
        return submissionRepository.findWithAnswersByPostIdAndMemberId(postId, memberId)
                .map(SubmissionView::withAnswers);
    }

    /** 제출 상세 (답변 포함). 다른 글의 제출이면 비어 있다 */
    public Optional<SubmissionView> findDetail(Long postId, Long submissionId) {
        return submissionRepository.findWithAnswersByIdAndPostId(submissionId, postId)
                .map(SubmissionView::withAnswers);
    }

    /** 제출자 목록 한 페이지 (답변 없음). status가 null이면 전체 */
    public List<SubmissionView> findPage(Long postId, SubmissionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
        List<Submission> rows = status == null
                ? submissionRepository.findByPostId(postId, pageable)
                : submissionRepository.findByPostIdAndStatus(postId, status, pageable);
        return rows.stream().map(SubmissionView::withoutAnswers).toList();
    }

    /** 글의 모든 제출 (답변 포함, CSV용) */
    public List<SubmissionView> findAll(Long postId) {
        return submissionRepository.findWithAnswersByPostId(postId, NEWEST_FIRST).stream()
                .map(SubmissionView::withAnswers)
                .toList();
    }

    /** 글 전체의 상태별 제출 수 */
    public Counts counts(Long postId) {
        long submitted = submissionRepository.countByPostIdAndStatus(postId, SubmissionStatus.SUBMITTED);
        long completed = submissionRepository.countByPostIdAndStatus(postId, SubmissionStatus.COMPLETED);
        return new Counts(submitted + completed, submitted, completed);
    }

    public record Counts(long total, long submitted, long completed) {
    }
}
