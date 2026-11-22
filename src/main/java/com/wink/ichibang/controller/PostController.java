package com.wink.ichibang.controller;

import com.wink.ichibang.dto.response.PostDetailResponse;
import com.wink.ichibang.dto.response.PostPageResponse;
import com.wink.ichibang.dto.response.PostSummaryResponse;
import com.wink.ichibang.service.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 누구나(비로그인 포함) 호출 가능 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    /** P-1 공개 목록 (POST-04) */
    @GetMapping
    public PostPageResponse<PostSummaryResponse> getPosts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return postService.getPosts(page, size);
    }

    /** P-2 공개 상세 (POST-05) */
    @GetMapping("/{postId}")
    public PostDetailResponse getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }
}
