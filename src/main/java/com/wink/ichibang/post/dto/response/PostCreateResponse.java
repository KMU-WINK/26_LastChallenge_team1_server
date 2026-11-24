package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.DisplayStatus;
import com.wink.ichibang.post.domain.Post;

import java.time.LocalDateTime;

/** POST-01 응답 */
public record PostCreateResponse(
        Long postId,
        DisplayStatus status,
        int questionCount
) {
    public static PostCreateResponse of(Post post, LocalDateTime now) {
        return new PostCreateResponse(post.getId(), post.displayStatus(now), post.getQuestions().size());
    }
}
