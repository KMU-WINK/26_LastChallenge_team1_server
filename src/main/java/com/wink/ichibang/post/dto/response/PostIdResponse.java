package com.wink.ichibang.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;

import java.time.LocalDateTime;

/** 등록, 게시, 마감 응답. 값이 없는 필드는 JSON에서 뺀다 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostIdResponse(
        Long postId,
        PostStatus status,
        Boolean visible,
        LocalDateTime publishedAt
) {
    public static PostIdResponse created(Post post) {
        return new PostIdResponse(post.getId(), post.getStatus(), null, null);
    }

    public static PostIdResponse published(Post post, LocalDateTime now) {
        return new PostIdResponse(post.getId(), post.effectiveStatus(now), post.isVisible(now),
                post.getPublishedAt());
    }

    public static PostIdResponse closed(Post post) {
        return new PostIdResponse(post.getId(), post.getStatus(), null, null);
    }
}
