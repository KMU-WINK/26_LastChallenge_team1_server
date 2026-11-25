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
import org.springframework.transaction.annotation.Isolation;
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

    /** submission 테이블의 (post_id, member_id) 유니크 제약 이름 (V1__init.sql, Submission 엔티티와 같다) */
    private static final String UNIQUE_POST_MEMBER = "uk_submission_post_member";

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
            // 위 existsBy 확인과 저장 사이에 같은 부원의 요청이 먼저 저장된 경우 (유니크 제약).
            // 다른 제약 위반까지 "중복"으로 가리지 않도록 이 제약일 때만 바꾼다
            if (!violatesUniquePostMember(e)) {
                throw e;
            }
            throw new SubmissionException(SubmissionErrorCode.DUPLICATE_SUBMISSION);
        }
        return SubmissionCreatedResponse.of(submission);
    }

    /**
     * SUB-05 답변 수정. 기존 답변을 모두 지우고 새로 저장한다. 제출 일시는 유지, 수정 일시만 갱신.
     * READ_COMMITTED: 행 잠금을 기다리는 동안 앞 요청이 커밋한 답변 변경을 잠금 뒤에 읽어야 한다.
     * 기본(REPEATABLE READ)이면 트랜잭션 시작 시점 스냅샷을 읽어 이미 지워진 답변을 또 지우려다 실패한다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

    /** SUB-03 취소. 제출과 답변을 삭제하고 이력은 남기지 않는다. READ_COMMITTED인 이유는 update와 같다 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(Long postId, Long memberId) {
        applyPolicy.requireApplicable(postId);
        Submission submission = findMine(postId, memberId);
        submission.requireChangeable();
        submissionRepository.delete(submission);
    }

    private static boolean violatesUniquePostMember(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(UNIQUE_POST_MEMBER)) {
                return true;
            }
        }
        return false;
    }

    /** 수정·취소 전용. 행을 잠가서 같은 신청서를 동시에 바꾸거나 운영진 처리와 겹치는 것을 막는다 */
    private Submission findMine(Long postId, Long memberId) {
        return submissionRepository.findForUpdateByPostIdAndMemberId(postId, memberId)
                .orElseThrow(() -> new SubmissionException(SubmissionErrorCode.SUBMISSION_NOT_FOUND));
    }

    /** 명세 예시처럼 초 단위로 다룬다 */
    private LocalDateTime now() {
        return LocalDateTime.now(Post.KST).truncatedTo(ChronoUnit.SECONDS);
    }
}
