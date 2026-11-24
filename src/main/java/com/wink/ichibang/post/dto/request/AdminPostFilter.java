package com.wink.ichibang.post.dto.request;

/** 운영진 목록 탭 (POST-04) */
public enum AdminPostFilter {
    ALL,      // 전체 (모집 전 포함)
    OPEN,     // 모집중
    CLOSED    // 마감
}
