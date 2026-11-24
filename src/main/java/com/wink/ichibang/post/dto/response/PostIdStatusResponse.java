package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.PostStatus;

/** POST-01 작성, POST-02 수정, POST-06 마감 응답 { id, status } */
public record PostIdStatusResponse(Long id, PostStatus status) {

    public static PostIdStatusResponse of(Long id, PostStatus status) {
        return new PostIdStatusResponse(id, status);
    }
}
