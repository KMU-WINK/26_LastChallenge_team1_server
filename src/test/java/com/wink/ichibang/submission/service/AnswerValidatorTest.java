package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.submission.domain.AnswerDraft;
import com.wink.ichibang.submission.dto.request.SubmissionRequest.AnswerRequest;
import com.wink.ichibang.global.exception.ErrorCode;
import com.wink.ichibang.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.wink.ichibang.submission.service.SubmissionTestFixtures.LONG_Q;
import static com.wink.ichibang.submission.service.SubmissionTestFixtures.MULTI_Q;
import static com.wink.ichibang.submission.service.SubmissionTestFixtures.SHORT_Q;
import static com.wink.ichibang.submission.service.SubmissionTestFixtures.SINGLE_Q;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerValidatorTest {

    private final AnswerValidator validator = new AnswerValidator();
    private final Post post = SubmissionTestFixtures.openPost();

    private static AnswerRequest choice(long questionId, Long... optionIds) {
        return new AnswerRequest(questionId, List.of(optionIds), null);
    }

    private static AnswerRequest text(long questionId, String text) {
        return new AnswerRequest(questionId, null, text);
    }

    private void assertInvalid(List<AnswerRequest> requests) {
        assertThatThrownBy(() -> validator.validate(post, requests))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER);
                    assertThat(e.getFieldErrors()).isNotEmpty();
                });
    }

    @Test
    void 답변이_없어도_통과한다() {
        assertThat(validator.validate(post, List.of())).isEmpty();
    }

    @Test
    void 모든_유형의_정상_답변을_저장_값으로_바꾼다() {
        List<AnswerDraft> drafts = validator.validate(post, List.of(
                choice(SINGLE_Q, 12L),
                choice(MULTI_Q, 21L, 22L),
                text(SHORT_Q, "github.com/haein-kim"),
                text(LONG_Q, "첫 줄\n둘째 줄")));

        assertThat(drafts).containsExactly(
                new AnswerDraft(SINGLE_Q, List.of(12L), null),
                new AnswerDraft(MULTI_Q, List.of(21L, 22L), null),
                new AnswerDraft(SHORT_Q, List.of(), "github.com/haein-kim"),
                new AnswerDraft(LONG_Q, List.of(), "첫 줄\n둘째 줄"));
    }

    @Test
    void 비어_있는_답변은_답변_없음으로_건너뛴다() {
        List<AnswerDraft> drafts = validator.validate(post, List.of(
                new AnswerRequest(SINGLE_Q, null, null),
                new AnswerRequest(MULTI_Q, List.of(), null),
                text(SHORT_Q, "   "),
                text(LONG_Q, null)));

        assertThat(drafts).isEmpty();
    }

    @Test
    void 다른_글의_질문이면_INVALID_ANSWER() {
        assertInvalid(List.of(text(999L, "x")));
    }

    @Test
    void 같은_질문에_두_번_답하면_INVALID_ANSWER() {
        assertInvalid(List.of(text(SHORT_Q, "a"), text(SHORT_Q, "b")));
    }

    @Test
    void 다른_질문의_선택지이면_INVALID_ANSWER() {
        assertInvalid(List.of(choice(SINGLE_Q, 21L)));
        assertInvalid(List.of(choice(SINGLE_Q, 999L)));
    }

    @Test
    void 단일_질문에_여러_개를_고르면_INVALID_ANSWER() {
        assertInvalid(List.of(choice(SINGLE_Q, 11L, 12L)));
    }

    @Test
    void 복수_질문에_같은_선택지를_두_번_고르면_INVALID_ANSWER() {
        assertInvalid(List.of(choice(MULTI_Q, 21L, 21L)));
    }

    @Test
    void 선택지_목록에_null이_있으면_INVALID_ANSWER() {
        assertInvalid(List.of(new AnswerRequest(MULTI_Q, java.util.Arrays.asList(21L, null), null)));
    }

    @Test
    void 객관식에_글로_답하면_INVALID_ANSWER() {
        assertInvalid(List.of(new AnswerRequest(SINGLE_Q, List.of(11L), "직접 입력")));
        assertInvalid(List.of(text(MULTI_Q, "직접 입력")));
    }

    @Test
    void 단답_서술에_선택지를_보내면_INVALID_ANSWER() {
        assertInvalid(List.of(new AnswerRequest(SHORT_Q, List.of(11L), null)));
        assertInvalid(List.of(new AnswerRequest(LONG_Q, List.of(11L), "글")));
    }

    @Test
    void 글자_수_제한은_경계값까지_허용한다() {
        String shortMax = "a".repeat(AnswerValidator.SHORT_MAX_LENGTH);
        String longMax = "a".repeat(AnswerValidator.LONG_MAX_LENGTH);
        assertThat(validator.validate(post, List.of(text(SHORT_Q, shortMax), text(LONG_Q, longMax)))).hasSize(2);

        assertInvalid(List.of(text(SHORT_Q, shortMax + "a")));
        assertInvalid(List.of(text(LONG_Q, longMax + "a")));
    }
}
