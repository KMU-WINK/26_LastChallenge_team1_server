package com.wink.ichibang.dto.response;

import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;
import com.wink.ichibang.domain.PostType;

import java.time.LocalDateTime;

/** P-2 공개 상세 */
public record PostDetailResponse(
        Long postId,
        String term,
        PostType type,
        String title,
        String description,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        PostStatus status,
        Integer dDay,
        boolean submittable,
        int itemCount
) {
    public static PostDetailResponse of(Post post, int itemCount, LocalDateTime now) {
        return new PostDetailResponse(
                post.getId(),
                post.getTerm(),
                post.getType(),
                post.getTitle(),
                post.getDescription(),
                post.getOpensAt(),
                post.getClosesAt(),
                post.effectiveStatus(now),
                post.dDay(now),
                post.isSubmittable(now),
                itemCount
        );
    }
}
