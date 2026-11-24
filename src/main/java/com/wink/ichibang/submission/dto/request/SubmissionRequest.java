package com.wink.ichibang.submission.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 신청서 제출(SUB-01)·수정(SUB-05) 요청. 모든 질문은 선택 응답이라 answers가 비어도 된다.
 * 빠진 질문은 답변 없음이다.
 */
public record SubmissionRequest(
        @Valid List<@NotNull(message = "답변 항목이 비어 있어요") AnswerRequest> answers
) {
    public List<AnswerRequest> answersOrEmpty() {
        return answers == null ? List.of() : answers;
    }

    /**
     * @param optionIds 객관식에서 고른 선택지 ID. SINGLE은 최대 1개, 비우면 답변 없음
     * @param text      단답·서술 답변. 비우면 답변 없음
     */
    public record AnswerRequest(
            @NotNull(message = "질문 ID는 필수예요") Long questionId,
            List<Long> optionIds,
            String text
    ) {
    }
}
