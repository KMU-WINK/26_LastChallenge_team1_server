package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.submission.domain.AnswerDraft;
import com.wink.ichibang.submission.domain.Submission;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import com.wink.ichibang.submission.dto.request.SubmissionRequest;
import com.wink.ichibang.submission.dto.request.SubmissionRequest.AnswerRequest;
import com.wink.ichibang.submission.dto.response.SubmissionCreatedResponse;
import com.wink.ichibang.submission.dto.response.SubmissionUpdatedResponse;
import com.wink.ichibang.global.exception.ErrorCode;
import com.wink.ichibang.global.exception.BusinessException;
import com.wink.ichibang.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    private static final long POST_ID = 100L;
    private static final long MEMBER_ID = 7L;

    @Mock
    SubmissionRepository submissionRepository;
    @Mock
    ApplyPolicy applyPolicy;

    SubmissionService service;
    Post post;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(submissionRepository, applyPolicy, new AnswerValidator());
        post = SubmissionTestFixtures.openPost();
    }

    private static SubmissionRequest request(AnswerRequest... answers) {
        return new SubmissionRequest(List.of(answers));
    }

    private static AnswerRequest shortAnswer(String text) {
        return new AnswerRequest(SubmissionTestFixtures.SHORT_Q, null, text);
    }

    private void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private Submission existing(SubmissionStatus status, String text) {
        Submission submission = Submission.create(POST_ID, MEMBER_ID,
                List.of(new AnswerDraft(SubmissionTestFixtures.SHORT_Q, List.of(), text)),
                LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(submission, "id", 11L);
        ReflectionTestUtils.setField(submission, "status", status);
        return submission;
    }

    // ===== SUB-01 =====

    @Test
    void 제출하면_SUBMITTED로_저장하고_답변을_함께_담는다() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.existsByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(false);

        SubmissionCreatedResponse response = service.submit(POST_ID, MEMBER_ID, request(shortAnswer("답")));

        var captor = org.mockito.ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).saveAndFlush(captor.capture());
        Submission saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(saved.getPostId()).isEqualTo(POST_ID);
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getAnswers()).hasSize(1);
        assertThat(response.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(response.submittedAt().getNano()).isZero();
    }

    @Test
    void 모든_질문을_비워도_제출된다() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);

        service.submit(POST_ID, MEMBER_ID, new SubmissionRequest(null));

        var captor = org.mockito.ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAnswers()).isEmpty();
    }

    @Test
    void 모집_중이_아니면_저장하지_않는다() {
        when(applyPolicy.requireApplicable(POST_ID))
                .thenThrow(new BusinessException(ErrorCode.APPLY_POST_NOT_OPEN));

        assertCode(() -> service.submit(POST_ID, MEMBER_ID, request()), ErrorCode.APPLY_POST_NOT_OPEN);
        verify(submissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_제출했으면_DUPLICATE_SUBMISSION() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.existsByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(true);

        assertCode(() -> service.submit(POST_ID, MEMBER_ID, request()), ErrorCode.DUPLICATE_SUBMISSION);
        verify(submissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시_요청으로_유니크_제약에_걸리면_DUPLICATE_SUBMISSION() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        // 실제 MySQL은 원인 예외 메시지에 제약 이름을 담아 보낸다
        when(submissionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("Duplicate entry '100-7' for key 'submission.uk_submission_post_member'")));

        assertCode(() -> service.submit(POST_ID, MEMBER_ID, request()), ErrorCode.DUPLICATE_SUBMISSION);
    }

    @Test
    void 다른_무결성_오류는_중복으로_바꾸지_않고_그대로_던진다() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        DataIntegrityViolationException other = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("Cannot add or update a child row: a foreign key constraint fails"));
        when(submissionRepository.saveAndFlush(any())).thenThrow(other);

        assertThatThrownBy(() -> service.submit(POST_ID, MEMBER_ID, request())).isSameAs(other);
    }

    @Test
    void 잘못된_답변이면_INVALID_ANSWER이고_저장하지_않는다() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);

        assertCode(() -> service.submit(POST_ID, MEMBER_ID,
                request(new AnswerRequest(999L, null, "x"))), ErrorCode.INVALID_ANSWER);
        verify(submissionRepository, never()).saveAndFlush(any());
    }

    // ===== SUB-05 =====

    @Test
    void 수정하면_기존_답변을_지우고_새로_저장하며_제출일시는_유지한다() {
        Submission submission = existing(SubmissionStatus.SUBMITTED, "이전 답");
        LocalDateTime submittedAt = submission.getSubmittedAt();
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.findForUpdateByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(Optional.of(submission));
        // flush 시점마다 메모리의 답변 수를 기록해 "삭제 반영 → 새 답변 추가" 순서를 확인한다
        List<Integer> answerCountAtFlush = new ArrayList<>();
        doAnswer(invocation -> answerCountAtFlush.add(submission.getAnswers().size()))
                .when(submissionRepository).flush();

        SubmissionUpdatedResponse response = service.update(POST_ID, MEMBER_ID, request(shortAnswer("새 답")));

        assertThat(submission.getAnswers()).hasSize(1);
        assertThat(submission.getAnswers().get(0).getText()).isEqualTo("새 답");
        assertThat(submission.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(submission.getUpdatedAt()).isAfter(submittedAt);
        assertThat(response.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        // 같은 질문을 다시 답해도 유니크 제약에 안 걸리도록, 기존 답변 삭제를 flush한 뒤에 새 답변을 붙인다
        assertThat(answerCountAtFlush).containsExactly(0, 1);
    }

    @Test
    void 수정할_제출이_없으면_SUBMISSION_NOT_FOUND() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.findForUpdateByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(Optional.empty());

        assertCode(() -> service.update(POST_ID, MEMBER_ID, request()), ErrorCode.SUBMISSION_NOT_FOUND);
    }

    @Test
    void 참여_완료된_제출은_수정할_수_없다() {
        Submission submission = existing(SubmissionStatus.COMPLETED, "이전 답");
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.findForUpdateByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(Optional.of(submission));

        assertCode(() -> service.update(POST_ID, MEMBER_ID, request(shortAnswer("새 답"))),
                ErrorCode.SUBMISSION_COMPLETED);
        assertThat(submission.getAnswers().get(0).getText()).isEqualTo("이전 답");
    }

    @Test
    void 수정할_때_잘못된_답변이면_기존_답변을_지우지_않는다() {
        Submission submission = existing(SubmissionStatus.SUBMITTED, "이전 답");
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.findForUpdateByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(Optional.of(submission));

        assertCode(() -> service.update(POST_ID, MEMBER_ID, request(new AnswerRequest(999L, null, "x"))),
                ErrorCode.INVALID_ANSWER);
        assertThat(submission.getAnswers()).hasSize(1);
    }

    @Test
    void 수정은_모집_중이_아니면_막힌다() {
        when(applyPolicy.requireApplicable(POST_ID))
                .thenThrow(new BusinessException(ErrorCode.APPLY_POST_NOT_OPEN));

        assertCode(() -> service.update(POST_ID, MEMBER_ID, request()), ErrorCode.APPLY_POST_NOT_OPEN);
        verify(submissionRepository, never()).findForUpdateByPostIdAndMemberId(any(), any());
    }

    // ===== SUB-03 =====

    @Test
    void 취소하면_제출을_삭제한다() {
        Submission submission = existing(SubmissionStatus.SUBMITTED, "답");
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.findForUpdateByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(Optional.of(submission));

        service.cancel(POST_ID, MEMBER_ID);

        verify(submissionRepository).delete(submission);
    }

    @Test
    void 취소할_제출이_없으면_SUBMISSION_NOT_FOUND() {
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.findForUpdateByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(Optional.empty());

        assertCode(() -> service.cancel(POST_ID, MEMBER_ID), ErrorCode.SUBMISSION_NOT_FOUND);
    }

    @Test
    void 참여_완료된_제출은_취소할_수_없다() {
        Submission submission = existing(SubmissionStatus.COMPLETED, "답");
        when(applyPolicy.requireApplicable(POST_ID)).thenReturn(post);
        when(submissionRepository.findForUpdateByPostIdAndMemberId(POST_ID, MEMBER_ID)).thenReturn(Optional.of(submission));

        assertCode(() -> service.cancel(POST_ID, MEMBER_ID), ErrorCode.SUBMISSION_COMPLETED);
        verify(submissionRepository, never()).delete(any(Submission.class));
    }

    @Test
    void 취소는_모집_중이_아니면_막힌다() {
        when(applyPolicy.requireApplicable(POST_ID))
                .thenThrow(new BusinessException(ErrorCode.APPLY_POST_NOT_OPEN));

        assertCode(() -> service.cancel(POST_ID, MEMBER_ID), ErrorCode.APPLY_POST_NOT_OPEN);
    }
}
