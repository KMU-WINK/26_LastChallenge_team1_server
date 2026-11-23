package com.wink.ichibang.post.dto.request;

import org.springframework.data.domain.Sort;

/** 공개 목록 정렬 (화면 오른쪽 위 드롭다운) */
public enum PostSort {

    LATEST(Sort.by(Sort.Direction.DESC, "id")),                       // 최신순 (기본)
    DEADLINE(Sort.by(Sort.Direction.ASC, "closesAt")                  // 마감임박순
            .and(Sort.by(Sort.Direction.DESC, "id")));

    private final Sort sort;

    PostSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
