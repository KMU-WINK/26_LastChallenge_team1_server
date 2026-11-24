package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.DisplayStatus;
import com.wink.ichibang.post.domain.Post;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 부원 목록의 한 줄 (POST-04): 번호, 제목, D-day, 한 줄 소개, 신청 기간, 상태 */
public record PostSummaryResponse(
        Long postId,
        String title,
        String summary,
        LocalDate opensOn,
        LocalDate closesOn,
        DisplayStatus status,
        Integer dDay
) {
    public static PostSummaryResponse of(Post post, LocalDateTime now) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getOpensOn(),
                post.getClosesOn(),
                post.displayStatus(now),
                post.dDay(now)
        );
    }
}
