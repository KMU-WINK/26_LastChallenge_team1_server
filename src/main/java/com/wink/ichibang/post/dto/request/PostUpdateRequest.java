package com.wink.ichibang.post.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * POST-02 신청글·신청서 수정. 보낸(null이 아닌) 항목만 바꾼다.
 * questions를 보내면 기존 질문 전체를 새 목록으로 교체한다.
 * summary에 빈 문자열을 보내면 한 줄 소개를 지운다.
 */
public record PostUpdateRequest(

        @Size(max = 100)
        String title,

        @Size(max = 100)
        String summary,

        @Size(max = 5000)
        String description,

        LocalDate opensOn,

        LocalDate closesOn,

        @Valid
        List<QuestionRequest> questions
) {
}
