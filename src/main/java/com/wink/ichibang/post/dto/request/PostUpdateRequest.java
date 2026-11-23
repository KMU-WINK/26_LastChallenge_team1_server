package com.wink.ichibang.post.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 신청글 수정. 보낸(null이 아닌) 필드만 바꾼다.
 * 상태별 수정 가능 여부는 Post 엔티티에서 검사한다.
 */
public record PostUpdateRequest(
        @Size(max = 100)
        String title,

        @Size(max = 100)
        String summary,

        LocalDateTime opensAt,

        LocalDateTime closesAt
) {
}
