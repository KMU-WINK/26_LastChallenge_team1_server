package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;

import java.time.LocalDateTime;

/** 운영진 기준 신청글의 상세, 수정 응답 */
public record AdminPostDetailResponse(
        Long postId,
        String title,
        String summary,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        PostStatus status,
        boolean visible,
        Integer dDay,
        boolean submittable,
        long submissionCount,
        LocalDateTime publishedAt,
        Long createdBy,          // 작성자 회원 ID (이름은 회원 기능이 생기면 추가)
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminPostDetailResponse of(Post post, long submissionCount, LocalDateTime now) {
        return new AdminPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getOpensAt(),
                post.getClosesAt(),
                post.effectiveStatus(now),
                post.isVisible(now),
                post.dDay(now),
                post.isSubmittable(now),
                submissionCount,
                post.getPublishedAt(),
                post.getCreatedBy(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
