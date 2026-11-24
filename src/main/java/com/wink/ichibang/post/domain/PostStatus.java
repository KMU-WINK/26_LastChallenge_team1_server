package com.wink.ichibang.post.domain;

/**
 * 신청글 상태 (POST-06). OPEN → CLOSED 한 방향.
 * DB에는 운영진의 수동 마감만 기록하고, 종료일이 지나면 조회할 때 CLOSED로 계산한다.
 */
public enum PostStatus {
    OPEN,    // 모집 중
    CLOSED   // 마감
}
