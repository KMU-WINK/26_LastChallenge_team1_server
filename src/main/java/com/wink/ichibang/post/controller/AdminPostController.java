package com.wink.ichibang.post.controller;

import com.wink.ichibang.auth.CurrentMember;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.global.response.ApiResponse;
import com.wink.ichibang.post.dto.request.PostCreateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest;
import com.wink.ichibang.post.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.post.dto.response.AdminPostListItemResponse;
import com.wink.ichibang.global.response.PageResponse;
import com.wink.ichibang.post.dto.response.PostIdStatusResponse;
import com.wink.ichibang.post.service.AdminPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 운영진용 (COM-01: /api/admin/** 는 운영진만).
 * 메서드마다 첫 줄에서 CurrentMember로 운영진인지 확인한다 (아니면 401/403).
 * 인증 필터가 생기면 필터가 대신 막으므로 그때 이 호출을 걷어내도 된다.
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;
    private final CurrentMember currentMember;

    /** POST-01 신청글 작성 (FORM-01 질문 포함) → 201 + Location */
    @PostMapping
    public ResponseEntity<ApiResponse<PostIdStatusResponse>> create(@Valid @RequestBody PostCreateRequest request) {
        long adminId = currentMember.requireAdminId();
        PostIdStatusResponse response = adminPostService.create(request, adminId);
        return ResponseEntity.created(URI.create("/api/admin/posts/" + response.id()))
                .body(ApiResponse.of(response));
    }

    /** POST-07 운영진 신청글 목록. status 없으면 전체 */
    @GetMapping
    public ApiResponse<PageResponse<AdminPostListItemResponse>> getPosts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이에요") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1~100이에요")
            @Max(value = 100, message = "size는 1~100이에요") int size) {
        currentMember.requireAdminId();
        return ApiResponse.of(adminPostService.getPosts(status, type, keyword, page, size));
    }

    /** POST-02 수정 화면용 조회 */
    @GetMapping("/{postId}")
    public ApiResponse<AdminPostDetailResponse> getPost(@PathVariable Long postId) {
        currentMember.requireAdminId();
        return ApiResponse.of(adminPostService.getPost(postId));
    }

    /** POST-02 신청글 수정 */
    @PutMapping("/{postId}")
    public ApiResponse<PostIdStatusResponse> update(@PathVariable Long postId,
                                                    @Valid @RequestBody PostUpdateRequest request) {
        currentMember.requireAdminId();
        return ApiResponse.of(adminPostService.update(postId, request));
    }

    /** POST-03 신청글 삭제 → 204 */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId) {
        currentMember.requireAdminId();
        adminPostService.delete(postId);
        return ResponseEntity.noContent().build();
    }

    /** POST-06 수동 마감 */
    @PostMapping("/{postId}/close")
    public ApiResponse<PostIdStatusResponse> close(@PathVariable Long postId) {
        currentMember.requireAdminId();
        return ApiResponse.of(adminPostService.close(postId));
    }
}
