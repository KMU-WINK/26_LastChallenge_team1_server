package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;

import java.time.LocalDateTime;

/** 운영진 기준 화면 목록의 한 줄 */
public record AdminPostSummaryResponse(
        Long postId,
        String title,
        String summary,
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
                post.getTitle(),
                post.getSummary(),
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
