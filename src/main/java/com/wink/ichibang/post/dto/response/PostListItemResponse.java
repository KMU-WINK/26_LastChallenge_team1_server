package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;

import java.time.LocalDate;

/** POST-04 부원 목록 한 줄 */
public record PostListItemResponse(
        long no,
        Long id,
        PostType type,
        String title,
        String summary,
        LocalDate startDate,
        LocalDate endDate,
        PostStatus status,
        Integer dDay
) {
    public static PostListItemResponse of(Post post, long no, LocalDate today) {
        return new PostListItemResponse(
                no,
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getSummary(),
                post.getStartDate(),
                post.getEndDate(),
                post.statusOn(today),
                post.dDayOn(today)
        );
    }
}
