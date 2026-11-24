package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.domain.QuestionOption;
import com.wink.ichibang.post.domain.QuestionType;

import java.time.LocalDate;
import java.util.List;

/** POST-02 수정 화면용 조회. POST-01 요청 Body 모양 + id, status, submissionCount */
public record AdminPostDetailResponse(
        Long id,
        PostType type,
        String title,
        String summary,
        LocalDate startDate,
        LocalDate endDate,
        String content,
        PostStatus status,
        long submissionCount,
        List<QuestionResponse> questions
) {
    public static AdminPostDetailResponse of(Post post, LocalDate today, long submissionCount) {
        return new AdminPostDetailResponse(
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getSummary(),
                post.getStartDate(),
                post.getEndDate(),
                post.getContent(),
                post.statusOn(today),
                submissionCount,
                post.getQuestions().stream().map(QuestionResponse::of).toList()
        );
    }

    /** options는 SHORT·LONG이면 빈 배열 */
    public record QuestionResponse(
            Long id,
            String content,
            String description,
            QuestionType type,
            List<OptionResponse> options
    ) {
        static QuestionResponse of(Question q) {
            return new QuestionResponse(q.getId(), q.getContent(), q.getDescription(), q.getType(),
                    q.getOptions().stream().map(OptionResponse::of).toList());
        }
    }

    public record OptionResponse(Long id, String content) {
        static OptionResponse of(QuestionOption o) {
            return new OptionResponse(o.getId(), o.getContent());
        }
    }
}
