package com.wink.ichibang.post.controller;

import com.wink.ichibang.post.dto.request.AdminPostFilter;
import com.wink.ichibang.post.dto.request.PostCreateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest;
import com.wink.ichibang.post.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.post.dto.response.AdminPostSummaryResponse;
import com.wink.ichibang.post.dto.response.PostCloseResponse;
import com.wink.ichibang.post.dto.response.PostCreateResponse;
import com.wink.ichibang.post.dto.response.PostDeleteResponse;
import com.wink.ichibang.post.dto.response.PostPageResponse;
import com.wink.ichibang.post.service.AdminPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 운영진 전용.
 * TODO: 권한 검사와 로그인 회원 ID는 팀에서 인증 방식을 정한 뒤 붙인다.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    /** TODO: 인증이 붙으면 로그인한 회원 ID로 교체. 그 전까지 작성자는 1번 회원 */
    private static final Long TEMP_MEMBER_ID = 1L;

    private final AdminPostService adminPostService;

    /** POST-04 운영진 목록. status: ALL(기본) / OPEN / CLOSED */
    @GetMapping
    public PostPageResponse<AdminPostSummaryResponse> getPosts(
            @RequestParam(defaultValue = "ALL") AdminPostFilter status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return adminPostService.getPosts(status, page, size);
    }

    /** 운영진 상세 (수정 화면용, 질문 포함) */
    @GetMapping("/{postId}")
    public AdminPostDetailResponse getPost(@PathVariable Long postId) {
        return adminPostService.getPost(postId);
    }

    /** POST-01 작성·게시 (FORM-01 질문 포함) → 201 Created */
    @PostMapping
    public ResponseEntity<PostCreateResponse> create(@Valid @RequestBody PostCreateRequest request) {
        PostCreateResponse response = adminPostService.create(request, TEMP_MEMBER_ID);
        return ResponseEntity
                .created(URI.create("/api/admin/posts/" + response.postId()))
                .body(response);
    }

    /** POST-02 수정 */
    @PatchMapping("/{postId}")
    public AdminPostDetailResponse update(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request) {
        return adminPostService.update(postId, request);
    }

    /** POST-03 삭제. 제출자가 있으면 마감 처리 (응답의 result로 구분) */
    @DeleteMapping("/{postId}")
    public PostDeleteResponse delete(@PathVariable Long postId) {
        return adminPostService.delete(postId);
    }

    /** POST-06 수동 마감 */
    @PostMapping("/{postId}/close")
    public PostCloseResponse close(@PathVariable Long postId) {
        return adminPostService.close(postId);
    }
}
