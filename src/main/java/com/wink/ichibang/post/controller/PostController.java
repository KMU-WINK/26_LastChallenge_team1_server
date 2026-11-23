package com.wink.ichibang.post.controller;

import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.dto.request.PostSort;
import com.wink.ichibang.post.dto.response.PostDetailResponse;
import com.wink.ichibang.post.dto.response.PostPageResponse;
import com.wink.ichibang.post.dto.response.PostSummaryResponse;
import com.wink.ichibang.post.service.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 누구나(비로그인 포함) 호출 가능 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    /**
     * 공개 목록.
     * 예: /api/posts?status=OPEN&type=STUDY&sort=LATEST&page=0&size=10
     *
     * @param status 탭: OPEN(모집중, 기본) / CLOSED(마감)
     * @param sort   정렬: LATEST(최신순, 기본) / DEADLINE(마감임박순)
     */
    @GetMapping
    public PostPageResponse<PostSummaryResponse> getPosts(
            @RequestParam(defaultValue = "OPEN") PostStatus status,
            @RequestParam(defaultValue = "LATEST") PostSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return postService.getPosts(status, sort, page, size);
    }

    /** 공개 상세 */
    @GetMapping("/{postId}")
    public PostDetailResponse getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }
}
