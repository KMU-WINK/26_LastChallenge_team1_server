package com.wink.ichibang.post.domain;

/**
 * DB에 저장되는 신청글 상태.
 * 실제로 보여 줄 상태는 Post#effectiveStatus(now)로 계산한다.
 */
public enum PostStatus {
    DRAFT,   // 임시 저장 (운영진만 보임)
    OPEN,    // 게시됨. opensAt부터 부원에게 보이고, closesAt이 지나면 계산상 CLOSED
    CLOSED   // 수동 마감
}
