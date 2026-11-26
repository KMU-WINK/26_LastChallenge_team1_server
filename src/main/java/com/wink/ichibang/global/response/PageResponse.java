package com.wink.ichibang.global.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.BiFunction;

/** 목록 응답 { content, page, size, totalElements, totalPages } */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * @param mapper (원소, 목록 번호 no) → 응답 원소.
     *               no는 전체 기준 최신 글이 가장 큰 번호 (totalElements - 앞선 개수).
     */
    public static <E, T> PageResponse<T> of(Page<E> page, BiFunction<E, Long, T> mapper) {
        long first = page.getTotalElements() - (long) page.getNumber() * page.getSize();
        List<E> items = page.getContent();
        List<T> content = new java.util.ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            content.add(mapper.apply(items.get(i), first - i));
        }
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
