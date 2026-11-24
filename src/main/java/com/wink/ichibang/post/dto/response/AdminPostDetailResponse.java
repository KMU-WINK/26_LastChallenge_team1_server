package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.DisplayStatus;
import com.wink.ichibang.post.domain.Post;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 운영진 상세 (수정 화면용), 수정 응답 (POST-02).
 * editableQuestions가 false면 프론트는 질문 영역을 잠근다.
 */
public record AdminPostDetailResponse(
        Long postId,
        String title,
        String summary,
        String description,
        LocalDate opensOn,
        LocalDate closesOn,
        DisplayStatus status,
        Integer dDay,
        long submissionCount,
        boolean editableQuestions,
        List<QuestionResponse> questions,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminPostDetailResponse of(Post post, long submissionCount, LocalDateTime now) {
        boolean editableQuestions = submissionCount == 0
                && post.displayStatus(now) != DisplayStatus.CLOSED;

        return new AdminPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getDescription(),
                post.getOpensOn(),
                post.getClosesOn(),
                post.displayStatus(now),
                post.dDay(now),
                submissionCount,
                editableQuestions,
                post.getQuestions().stream().map(QuestionResponse::from).toList(),
                post.getCreatedBy(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
