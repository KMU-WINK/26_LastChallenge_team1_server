package com.wink.ichibang.dto.request;

import com.wink.ichibang.domain.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** P-5 신청글 등록 */
public record PostCreateRequest(

        @NotBlank
        @Pattern(regexp = "^\\d{4}-[12]$", message = "학기는 2026-1 형식이어야 합니다.")
        String term,

        @NotNull
        PostType type,

        @NotBlank
        @Size(max = 100)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull
        LocalDateTime opensAt,

        @NotNull
        LocalDateTime closesAt
) {
}
