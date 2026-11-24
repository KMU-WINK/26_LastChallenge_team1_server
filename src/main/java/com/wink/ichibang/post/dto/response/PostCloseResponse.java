package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.DisplayStatus;

/** POST-06 수동 마감 응답 */
public record PostCloseResponse(Long postId, DisplayStatus status) {

    public static PostCloseResponse of(Long postId) {
        return new PostCloseResponse(postId, DisplayStatus.CLOSED);
    }
}
