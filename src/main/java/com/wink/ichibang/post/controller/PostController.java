package com.wink.ichibang.post.controller;

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

/** 부원용. 누구나(비로그인 포함) 호출 가능 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    /**
     * POST-04 부원 목록.
     * 예: /api/posts?openOnly=true&page=0&size=10
     *
     * @param openOnly "모집 중인 게시글만 보기" 토글
     */
    @GetMapping
    public PostPageResponse<PostSummaryResponse> getPosts(
            @RequestParam(defaultValue = "false") boolean openOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return postService.getPosts(openOnly, page, size);
    }

    /** POST-05 부원 상세 */
    @GetMapping("/{postId}")
    public PostDetailResponse getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }
}
