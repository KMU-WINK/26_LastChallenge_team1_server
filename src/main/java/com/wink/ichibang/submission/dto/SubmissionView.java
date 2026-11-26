package com.wink.ichibang.submission.dto;

import com.wink.ichibang.submission.domain.Answer;
import com.wink.ichibang.submission.domain.Submission;
import com.wink.ichibang.submission.domain.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 다른 도메인(form)이 제출을 읽을 때 쓰는 값. 엔티티와 테이블 구조를 밖으로 내보내지 않는다.
 * 답변이 필요 없는 조회(목록 등)는 answers가 빈 목록이다.
 */
public record SubmissionView(
        Long id,
        Long memberId,
        SubmissionStatus status,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        List<AnswerView> answers
) {

    /** 질문 하나에 대한 답. 객관식은 optionIds, 단답·서술은 text */
    public record AnswerView(Long questionId, String text, List<Long> optionIds) {
    }

    public static SubmissionView withoutAnswers(Submission submission) {
        return of(submission, List.of());
    }

    /** 트랜잭션 안에서 불러야 한다 (답변은 지연 로딩) */
    public static SubmissionView withAnswers(Submission submission) {
        return of(submission, submission.getAnswers().stream().map(SubmissionView::toView).toList());
    }

    private static SubmissionView of(Submission submission, List<AnswerView> answers) {
        return new SubmissionView(submission.getId(), submission.getMemberId(), submission.getStatus(),
                submission.getSubmittedAt(), submission.getCompletedAt(), answers);
    }

    private static AnswerView toView(Answer answer) {
        return new AnswerView(answer.getQuestionId(), answer.getText(), List.copyOf(answer.getOptionIds()));
    }
}
