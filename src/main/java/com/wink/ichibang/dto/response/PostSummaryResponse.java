package com.wink.ichibang.dto.response;

import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;
import com.wink.ichibang.domain.PostType;

import java.time.LocalDateTime;

/** P-1 공개 목록의 한 줄 */
public record PostSummaryResponse(
        Long postId,
        String term,
        PostType type,
        String title,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        PostStatus status,
        Integer dDay
) {
    public static PostSummaryResponse of(Post post, LocalDateTime now) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTerm(),
                post.getType(),
                post.getTitle(),
                post.getOpensAt(),
                post.getClosesAt(),
                post.effectiveStatus(now),
                post.dDay(now)
        );
    }
}
