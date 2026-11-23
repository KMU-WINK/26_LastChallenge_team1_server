package com.wink.ichibang.post.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/** 페이지 응답: content, page, size, totalElements, hasNext */
public record PostPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
    public static <T> PostPageResponse<T> from(Page<T> page) {
        return new PostPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.hasNext()
        );
    }
}
