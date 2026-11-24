package com.wink.ichibang.post.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * POST-01 신청글 작성·게시 (FORM-01 질문 포함).
 * 날짜는 "2026-11-16" 형식. 질문이 비어 있으면 FORM_EMPTY.
 */
public record PostCreateRequest(

        @NotBlank
        @Size(max = 100)
        String title,

        @Size(max = 100)
        String summary,

        @NotBlank
        @Size(max = 5000)
        String description,

        @NotNull
        LocalDate opensOn,

        @NotNull
        LocalDate closesOn,

        @Valid
        List<QuestionRequest> questions
) {
}
