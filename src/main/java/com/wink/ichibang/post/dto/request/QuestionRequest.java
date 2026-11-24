package com.wink.ichibang.post.dto.request;

import com.wink.ichibang.post.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 신청서 질문 하나 (FORM-01).
 * 선택지 개수·중복 규칙은 Question.create()에서 검사한다.
 */
public record QuestionRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotNull
        QuestionType type,

        List<String> options     // 객관식만. 단답형은 생략
) {
}
