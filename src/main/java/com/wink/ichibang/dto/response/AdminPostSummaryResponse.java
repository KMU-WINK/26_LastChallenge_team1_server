package com.wink.ichibang.dto.response;

import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;
import com.wink.ichibang.domain.PostType;

import java.time.LocalDateTime;

/** P-3 운영진 목록의 한 줄 */
public record AdminPostSummaryResponse(
        Long postId,
        String term,
        PostType type,
        String title,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        PostStatus status,
        boolean visible,
        LocalDateTime publishedAt,
        long submissionCount,
        LocalDateTime createdAt
) {
    public static AdminPostSummaryResponse of(Post post, long submissionCount, LocalDateTime now) {
        return new AdminPostSummaryResponse(
                post.getId(),
                post.getTerm(),
                post.getType(),
                post.getTitle(),
                post.getOpensAt(),
                post.getClosesAt(),
                post.effectiveStatus(now),
                post.isVisible(now),
                post.getPublishedAt(),
                submissionCount,
                post.getCreatedAt()
        );
    }
}
