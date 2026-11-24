package com.wink.ichibang.post.dto.request;

import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * POST-01 신청글 작성 (FORM-01 질문 포함).
 * 날짜는 "yyyy-MM-dd". 객관식 선택지 1개 이상 규칙은 서비스(QuestionDraftMapper)에서 검사한다.
 */
public record PostCreateRequest(
        @NotNull(message = "구분을 선택해주세요")
        PostType type,

        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 200, message = "제목은 200자까지 입력할 수 있어요")
        String title,

        @Size(max = 200, message = "한 줄 소개는 200자까지 입력할 수 있어요")
        String summary,

        @NotNull(message = "신청 시작일을 입력해주세요")
        LocalDate startDate,

        @NotNull(message = "신청 종료일을 입력해주세요")
        LocalDate endDate,

        @NotBlank(message = "본문을 입력해주세요")
        String content,

        /** 없거나 빈 배열이면 질문 0개. 배열 순서 = 표시 순서 */
        List<@Valid QuestionCreateRequest> questions
) {
    public record QuestionCreateRequest(
            @NotBlank(message = "질문을 입력해주세요")
            @Size(max = 200, message = "질문은 200자까지 입력할 수 있어요")
            String content,

            @Size(max = 500, message = "설명은 500자까지 입력할 수 있어요")
            String description,

            @NotNull(message = "질문 유형을 선택해주세요")
            QuestionType type,

            /** SINGLE·MULTI만. SHORT·LONG이면 보내도 무시한다 */
            List<@NotBlank(message = "선택지를 입력해주세요")
                 @Size(max = 200, message = "선택지는 200자까지 입력할 수 있어요") String> options
    ) {
    }
}
