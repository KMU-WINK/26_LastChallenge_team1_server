package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;

import java.time.LocalDateTime;

/** 공개 목록의 한 줄: 번호, 구분, 제목, 한 줄 설명, 신청 기간, 상태, D-day */
public record PostSummaryResponse(
        Long postId,
        String title,
        String summary,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        PostStatus status,
        Integer dDay
) {
    public static PostSummaryResponse of(Post post, LocalDateTime now) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getOpensAt(),
                post.getClosesAt(),
                post.effectiveStatus(now),
                post.dDay(now)
        );
    }
}
