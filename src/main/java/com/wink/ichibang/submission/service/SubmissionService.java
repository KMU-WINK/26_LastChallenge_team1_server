package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.submission.domain.AnswerDraft;
import com.wink.ichibang.submission.domain.Submission;
import com.wink.ichibang.submission.dto.request.SubmissionRequest;
import com.wink.ichibang.submission.dto.response.SubmissionCreatedResponse;
import com.wink.ichibang.submission.dto.response.SubmissionUpdatedResponse;
import com.wink.ichibang.submission.exception.SubmissionErrorCode;
import com.wink.ichibang.submission.exception.SubmissionException;
import com.wink.ichibang.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 신청서 제출·수정·취소 (SUB-01, SUB-05, SUB-03).
 * 세 기능 모두 먼저 SUB-02(ApplyPolicy)로 모집 중인지 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ApplyPolicy applyPolicy;
    private final AnswerValidator answerValidator;

    /** SUB-01 제출. 신청글당 1인 1제출, 제출과 답변은 한 트랜잭션으로 저장 */
    public SubmissionCreatedResponse submit(Long postId, Long memberId, SubmissionRequest request) {
        Post post = applyPolicy.requireApplicable(postId);

        if (submissionRepository.existsByPostIdAndMemberId(postId, memberId)) {
            throw new SubmissionException(SubmissionErrorCode.DUPLICATE_SUBMISSION);
        }
        List<AnswerDraft> drafts = answerValidator.validate(post, request.answersOrEmpty());

        Submission submission = Submission.create(postId, memberId, drafts, now());
        try {
            submissionRepository.saveAndFlush(submission);
        } catch (DataIntegrityViolationException e) {
            // 위 existsBy 확인과 저장 사이에 같은 부원의 요청이 먼저 저장된 경우 (유니크 제약)
            throw new SubmissionException(SubmissionErrorCode.DUPLICATE_SUBMISSION);
        }
        return SubmissionCreatedResponse.of(submission);
    }

    /** SUB-05 답변 수정. 기존 답변을 모두 지우고 새로 저장한다. 제출 일시는 유지, 수정 일시만 갱신 */
    public SubmissionUpdatedResponse update(Long postId, Long memberId, SubmissionRequest request) {
        Post post = applyPolicy.requireApplicable(postId);
        Submission submission = findMine(postId, memberId);
        submission.requireChangeable();

        List<AnswerDraft> drafts = answerValidator.validate(post, request.answersOrEmpty());

        // 같은 질문을 다시 답하면 유니크 제약(submission_id, question_id)에 걸리므로 삭제를 먼저 DB에 반영한다
        submission.clearAnswers();
        submissionRepository.flush();
        submission.addAnswers(drafts, now());
        submissionRepository.flush();
        return SubmissionUpdatedResponse.of(submission);
    }

    /** SUB-03 취소. 제출과 답변을 삭제하고 이력은 남기지 않는다 */
    public void cancel(Long postId, Long memberId) {
        applyPolicy.requireApplicable(postId);
        Submission submission = findMine(postId, memberId);
        submission.requireChangeable();
        submissionRepository.delete(submission);
    }

    private Submission findMine(Long postId, Long memberId) {
        return submissionRepository.findByPostIdAndMemberId(postId, memberId)
                .orElseThrow(() -> new SubmissionException(SubmissionErrorCode.SUBMISSION_NOT_FOUND));
    }

    /** 명세 예시처럼 초 단위로 다룬다 */
    private LocalDateTime now() {
        return LocalDateTime.now(Post.KST).truncatedTo(ChronoUnit.SECONDS);
    }
}
