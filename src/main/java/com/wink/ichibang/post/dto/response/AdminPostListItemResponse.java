package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;

import java.time.LocalDate;

/** POST-07 운영진 목록 한 줄 */
public record AdminPostListItemResponse(
        long no,
        Long id,
        PostType type,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        PostStatus status,
        long submissionCount
) {
    public static AdminPostListItemResponse of(Post post, long no, LocalDate today, long submissionCount) {
        return new AdminPostListItemResponse(
                no,
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getStartDate(),
                post.getEndDate(),
                post.statusOn(today),
                submissionCount
        );
    }
}
