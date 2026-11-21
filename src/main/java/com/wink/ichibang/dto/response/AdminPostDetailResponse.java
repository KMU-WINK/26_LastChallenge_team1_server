package com.wink.ichibang.dto.response;

import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;
import com.wink.ichibang.domain.PostType;

import java.time.LocalDateTime;

/** P-4 운영진 상세, P-8 수정 응답 */
public record AdminPostDetailResponse(
        Long postId,
        String term,
        PostType type,
        String title,
        String description,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        PostStatus status,
        boolean visible,
        Integer dDay,
        boolean submittable,
        int itemCount,
        long submissionCount,
        LocalDateTime publishedAt,
        Long createdBy,          // 작성자 회원 ID (이름은 member 도메인이 생기면 추가)
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminPostDetailResponse of(Post post, int itemCount, long submissionCount, LocalDateTime now) {
        return new AdminPostDetailResponse(
                post.getId(),
                post.getTerm(),
                post.getType(),
                post.getTitle(),
                post.getDescription(),
                post.getOpensAt(),
                post.getClosesAt(),
                post.effectiveStatus(now),
                post.isVisible(now),
                post.dDay(now),
                post.isSubmittable(now),
                itemCount,
                submissionCount,
                post.getPublishedAt(),
                post.getCreatedBy(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
