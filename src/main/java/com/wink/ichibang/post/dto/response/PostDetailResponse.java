package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;

import java.time.LocalDateTime;

/** 공개 상세 */
public record PostDetailResponse(
        Long postId,
        String title,
        String summary,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        PostStatus status,
        Integer dDay,
        boolean submittable
) {
    public static PostDetailResponse of(Post post, LocalDateTime now) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getOpensAt(),
                post.getClosesAt(),
                post.effectiveStatus(now),
                post.dDay(now),
                post.isSubmittable(now)
        );
    }
}
