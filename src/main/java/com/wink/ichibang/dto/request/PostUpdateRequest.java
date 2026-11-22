package com.wink.ichibang.dto.request;

import com.wink.ichibang.domain.PostType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * P-8 신청글 수정. 보낸(null이 아닌) 필드만 바꾼다.
 * 상태별 수정 가능 여부는 Post 엔티티에서 검사한다.
 */
public record PostUpdateRequest(

        @Pattern(regexp = "^\\d{4}-[12]$", message = "학기는 2026-1 형식이어야 합니다.")
        String term,

        PostType type,

        @Size(max = 100)
        String title,

        @Size(max = 2000)
        String description,

        LocalDateTime opensAt,

        LocalDateTime closesAt
) {
}
