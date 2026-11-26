package com.wink.ichibang.post.controller;

import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.global.response.ApiResponse;
import com.wink.ichibang.global.response.PageResponse;
import com.wink.ichibang.post.dto.response.PostDetailResponse;
import com.wink.ichibang.post.dto.response.PostListItemResponse;
import com.wink.ichibang.post.service.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 부원·비로그인용 (COM-01: 누구나 접근) */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** POST-04 신청글 목록 */
    @GetMapping
    public ApiResponse<PageResponse<PostListItemResponse>> getPosts(
            @RequestParam(defaultValue = "false") boolean recruiting,
            @RequestParam(required = false) PostType type,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이에요") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1~100이에요")
            @Max(value = 100, message = "size는 1~100이에요") int size) {
        return ApiResponse.of(postService.getPosts(recruiting, type, page, size));
    }

    /**
     * POST-05 신청글 상세.
     * TODO(인증): 로그인 연동 전까지 X-Member-Id 헤더로 부원을 흉내 낸다. 헤더가 없으면 비로그인.
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId) {
        return ApiResponse.of(postService.getPost(postId, memberId));
    }
}
