package com.wink.ichibang.post.domain;

/** 화면에 보일 상태 (기능 명세 POST-04의 상태 표) */
public enum DisplayStatus {
    UPCOMING,  // 모집 전: 오늘 < 시작일
    OPEN,      // 모집 중: 시작일 ≤ 오늘 ≤ 마감일, 수동 마감 아님
    CLOSED     // 마감: 마감일 지남 또는 수동 마감
}
