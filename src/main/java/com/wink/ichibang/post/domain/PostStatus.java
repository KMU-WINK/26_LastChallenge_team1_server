package com.wink.ichibang.post.domain;

/**
 * DB에 저장되는 신청글 상태. 운영진의 행동만 기록한다.
 * 화면에 보일 상태는 Post#displayStatus(now)로 계산한다.
 */
public enum PostStatus {
    OPEN,    // 게시됨 (작성 즉시)
    CLOSED   // 수동 마감
}
