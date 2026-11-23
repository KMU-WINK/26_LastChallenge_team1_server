package com.wink.ichibang.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 신청글 등록 */
public record PostCreateRequest(
        @NotBlank
        @Size(max = 100)
        String title,

        @NotBlank
        @Size(max = 100)
        String summary,

        @NotNull
        LocalDateTime opensAt,

        @NotNull
        LocalDateTime closesAt
) {
}
