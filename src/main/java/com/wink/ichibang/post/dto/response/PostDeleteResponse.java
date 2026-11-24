package com.wink.ichibang.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * POST-03 응답.
 * DELETED: 삭제됨 → 목록에서 행 제거
 * CLOSED : 제출자가 있어 삭제 대신 마감됨 → 행의 상태를 마감으로
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostDeleteResponse(
        Long postId,
        Result result,
        Long submissionCount
) {
    public enum Result { DELETED, CLOSED }

    public static PostDeleteResponse deleted(Long postId) {
        return new PostDeleteResponse(postId, Result.DELETED, null);
    }

    public static PostDeleteResponse closed(Long postId, long submissionCount) {
        return new PostDeleteResponse(postId, Result.CLOSED, submissionCount);
    }
}
