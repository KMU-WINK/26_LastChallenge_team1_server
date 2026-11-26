package com.wink.ichibang.submission.domain;

import com.wink.ichibang.global.exception.BusinessException;
import com.wink.ichibang.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 참여 완료 처리·되돌리기(FORM-04) 상태 규칙. DB 없이 엔티티만 확인한다 */
class SubmissionStatusTransitionTest {

    private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 11, 25, 14, 0);
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 11, 26, 9, 30);

    private static Submission submitted() {
        return Submission.create(100L, 7L, List.of(), SUBMITTED_AT);
    }

    private static void assertConflict(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void 제출됨을_완료로_바꾸면_완료_시각이_기록된다() {
        Submission submission = submitted();

        submission.complete(COMPLETED_AT);

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(submission.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(submission.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
        assertThat(submission.getUpdatedAt()).isEqualTo(SUBMITTED_AT); // 답변 수정 일시는 그대로
    }

    @Test
    void 완료를_되돌리면_제출됨이_되고_완료_시각이_사라진다() {
        Submission submission = submitted();
        submission.complete(COMPLETED_AT);

        submission.revert();

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submission.getCompletedAt()).isNull();
    }

    @Test
    void 이미_완료된_제출을_다시_완료하면_409() {
        Submission submission = submitted();
        submission.complete(COMPLETED_AT);

        assertConflict(() -> submission.complete(COMPLETED_AT.plusHours(1)));
        assertThat(submission.getCompletedAt()).isEqualTo(COMPLETED_AT); // 시각이 덮어써지지 않는다
    }

    @Test
    void 이미_제출됨인_제출을_되돌리면_409() {
        assertConflict(() -> submitted().revert());
    }

    @Test
    void 완료된_제출은_부원이_수정_취소할_수_없다() {
        Submission submission = submitted();
        submission.complete(COMPLETED_AT);

        assertThatThrownBy(submission::requireChangeable).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SUBMISSION_COMPLETED));
    }
}
