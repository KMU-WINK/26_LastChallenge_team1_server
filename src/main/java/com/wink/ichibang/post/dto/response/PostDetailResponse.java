package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.DisplayStatus;
import com.wink.ichibang.post.domain.Post;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 부원 상세 (POST-05). 작성자는 화면에 "WINK 운영진"으로 고정 표시하므로 보내지 않는다 */
public record PostDetailResponse(
        Long postId,
        String title,
        String summary,
        String description,
        LocalDate opensOn,
        LocalDate closesOn,
        DisplayStatus status,
        Integer dDay,
        boolean submittable,
        LocalDateTime createdAt
) {
    public static PostDetailResponse of(Post post, LocalDateTime now) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getDescription(),
                post.getOpensOn(),
                post.getClosesOn(),
                post.displayStatus(now),
                post.dDay(now),
                post.isSubmittable(now),
                post.getCreatedAt()
        );
    }
}
