package com.wink.ichibang.post.controller;

import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.dto.ApiResponse;
import com.wink.ichibang.post.dto.request.PostCreateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest;
import com.wink.ichibang.post.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.post.dto.response.AdminPostListItemResponse;
import com.wink.ichibang.post.dto.response.PageResponse;
import com.wink.ichibang.post.dto.response.PostIdStatusResponse;
import com.wink.ichibang.post.service.AdminPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** 운영진용 (COM-01: /api/admin/** 는 운영진만. 인증 필터가 붙으면 401/403은 필터가 처리) */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    /** TODO(인증): 로그인 연동 후 로그인한 운영진 ID로 바꾼다 */
    private static final Long TEMP_ADMIN_ID = 1L;

    private final AdminPostService adminPostService;

    /** POST-01 신청글 작성 (FORM-01 질문 포함) → 201 + Location */
    @PostMapping
    public ResponseEntity<ApiResponse<PostIdStatusResponse>> create(@Valid @RequestBody PostCreateRequest request) {
        PostIdStatusResponse response = adminPostService.create(request, TEMP_ADMIN_ID);
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
        return ApiResponse.of(adminPostService.getPosts(status, type, keyword, page, size));
    }

    /** POST-02 수정 화면용 조회 */
    @GetMapping("/{postId}")
    public ApiResponse<AdminPostDetailResponse> getPost(@PathVariable Long postId) {
        return ApiResponse.of(adminPostService.getPost(postId));
    }

    /** POST-02 신청글 수정 */
    @PutMapping("/{postId}")
    public ApiResponse<PostIdStatusResponse> update(@PathVariable Long postId,
                                                    @Valid @RequestBody PostUpdateRequest request) {
        return ApiResponse.of(adminPostService.update(postId, request));
    }

    /** POST-03 신청글 삭제 → 204 */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId) {
        adminPostService.delete(postId);
        return ResponseEntity.noContent().build();
    }

    /** POST-06 수동 마감 */
    @PostMapping("/{postId}/close")
    public ApiResponse<PostIdStatusResponse> close(@PathVariable Long postId) {
        return ApiResponse.of(adminPostService.close(postId));
    }
}
