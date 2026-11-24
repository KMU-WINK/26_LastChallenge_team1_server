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
 * POST-02 신청글 수정. 모양은 POST-01과 같고, 질문·선택지에 기존 id가 붙는다.
 * id 있음 = 유지·수정, id 없음 = 새로 추가, 요청에서 빠짐 = 삭제.
 * type(구분)은 바꿀 수 없다. 기존 값과 다르면 VALIDATION_FAILED.
 */
public record PostUpdateRequest(
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

        List<@Valid QuestionUpdateRequest> questions
) {
    public record QuestionUpdateRequest(
            Long id,

            @NotBlank(message = "질문을 입력해주세요")
            @Size(max = 200, message = "질문은 200자까지 입력할 수 있어요")
            String content,

            @Size(max = 500, message = "설명은 500자까지 입력할 수 있어요")
            String description,

            @NotNull(message = "질문 유형을 선택해주세요")
            QuestionType type,

            List<@Valid OptionUpdateRequest> options
    ) {
    }

    public record OptionUpdateRequest(
            Long id,

            @NotBlank(message = "선택지를 입력해주세요")
            @Size(max = 200, message = "선택지는 200자까지 입력할 수 있어요")
            String content
    ) {
    }
}
