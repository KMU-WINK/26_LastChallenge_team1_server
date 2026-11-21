package com.wink.ichibang.controller;

import com.wink.ichibang.domain.PostStatus;
import com.wink.ichibang.dto.request.PostCreateRequest;
import com.wink.ichibang.dto.request.PostUpdateRequest;
import com.wink.ichibang.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.dto.response.AdminPostSummaryResponse;
import com.wink.ichibang.dto.response.PostIdResponse;
import com.wink.ichibang.dto.response.PostPageResponse;
import com.wink.ichibang.service.AdminPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 운영진 전용.
 * TODO: 권한 검사(COM-01)와 로그인 회원 ID는 팀에서 인증 방식을 정한 뒤 붙인다.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    /** TODO: 인증이 붙으면 로그인한 회원 ID로 교체. 그 전까지 작성자는 1번 회원 */
    private static final Long TEMP_MEMBER_ID = 1L;

    private final AdminPostService adminPostService;

    /** P-3 운영진 목록 */
    @GetMapping
    public PostPageResponse<AdminPostSummaryResponse> getPosts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return adminPostService.getPosts(status, page, size);
    }

    /** P-4 운영진 상세 */
    @GetMapping("/{postId}")
    public AdminPostDetailResponse getPost(@PathVariable Long postId) {
        return adminPostService.getPost(postId);
    }

    /** P-5 등록 (POST-01) → 201 Created */
    @PostMapping
    public ResponseEntity<PostIdResponse> create(@Valid @RequestBody PostCreateRequest request) {
        PostIdResponse response = adminPostService.create(request, TEMP_MEMBER_ID);
        return ResponseEntity
                .created(URI.create("/api/admin/posts/" + response.postId()))
                .body(response);
    }

    /** P-6 게시 (POST-06) */
    @PostMapping("/{postId}/publish")
    public PostIdResponse publish(@PathVariable Long postId) {
        return adminPostService.publish(postId);
    }

    /** P-7 수동 마감 (POST-06 제안) */
    @PostMapping("/{postId}/close")
    public PostIdResponse close(@PathVariable Long postId) {
        return adminPostService.close(postId);
    }

    /** P-8 수정 (POST-02) */
    @PatchMapping("/{postId}")
    public AdminPostDetailResponse update(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request) {
        return adminPostService.update(postId, request);
    }

    /** P-9 삭제 (POST-03): 제출이 있으면 409 → 확인 후 ?confirm=true → 204 No Content */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "false") boolean confirm) {
        adminPostService.delete(postId, confirm);
        return ResponseEntity.noContent().build();
    }
}
