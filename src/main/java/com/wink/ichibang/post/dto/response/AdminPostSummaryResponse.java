package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.DisplayStatus;
import com.wink.ichibang.post.domain.Post;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 운영진 목록의 한 줄 (POST-04): 번호, 제목, 신청 기간, 상태, 제출 인원 */
public record AdminPostSummaryResponse(
        Long postId,
        String title,
        String summary,
        LocalDate opensOn,
        LocalDate closesOn,
        DisplayStatus status,
        Integer dDay,
        long submissionCount
) {
    public static AdminPostSummaryResponse of(Post post, long submissionCount, LocalDateTime now) {
        return new AdminPostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getOpensOn(),
                post.getClosesOn(),
                post.displayStatus(now),
                post.dDay(now),
                submissionCount
        );
    }
}
