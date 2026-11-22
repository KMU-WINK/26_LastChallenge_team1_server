package com.wink.ichibang.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;

import java.time.LocalDateTime;

/** P-5 등록, P-6 게시, P-7 마감 응답. 값이 없는 필드는 JSON에서 뺀다 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostIdResponse(
        Long postId,
        PostStatus status,
        Boolean visible,
        LocalDateTime publishedAt,
        Integer itemCount
) {
    public static PostIdResponse created(Post post, int itemCount) {
        return new PostIdResponse(post.getId(), post.getStatus(), null, null, itemCount);
    }

    public static PostIdResponse published(Post post, LocalDateTime now) {
        return new PostIdResponse(post.getId(), post.effectiveStatus(now), post.isVisible(now),
                post.getPublishedAt(), null);
    }

    public static PostIdResponse closed(Post post) {
        return new PostIdResponse(post.getId(), post.getStatus(), null, null, null);
    }
}
