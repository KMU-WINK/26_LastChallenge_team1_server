package com.wink.ichibang.submission.controller;

import com.wink.ichibang.post.dto.ApiResponse;
import com.wink.ichibang.submission.dto.request.SubmissionRequest;
import com.wink.ichibang.submission.dto.response.SubmissionCreatedResponse;
import com.wink.ichibang.submission.dto.response.SubmissionUpdatedResponse;
import com.wink.ichibang.submission.exception.SubmissionErrorCode;
import com.wink.ichibang.submission.exception.SubmissionException;
import com.wink.ichibang.submission.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부원 신청서 제출·수정·취소 (COM-01: 로그인 필요).
 * TODO(인증): 로그인 연동 전까지 PostController처럼 X-Member-Id 헤더로 부원을 흉내 낸다.
 *             헤더가 없으면 401. 인증 필터가 생기면 이 헤더 처리를 걷어낸다.
 */
@RestController
@RequestMapping("/api/posts/{postId}/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /** SUB-01 신청서 제출 → 201 */
    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionCreatedResponse>> submit(
            @PathVariable Long postId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId,
            @Valid @RequestBody(required = false) SubmissionRequest request) {
        SubmissionCreatedResponse response =
                submissionService.submit(postId, requireMember(memberId), orEmpty(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** SUB-05 답변 수정 → 200 */
    @PutMapping("/me")
    public ApiResponse<SubmissionUpdatedResponse> update(
            @PathVariable Long postId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId,
            @Valid @RequestBody(required = false) SubmissionRequest request) {
        return ApiResponse.of(submissionService.update(postId, requireMember(memberId), orEmpty(request)));
    }

    /** SUB-03 신청 취소 → 204 */
    @DeleteMapping("/me")
    public ResponseEntity<Void> cancel(
            @PathVariable Long postId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId) {
        submissionService.cancel(postId, requireMember(memberId));
        return ResponseEntity.noContent().build();
    }

    private static Long requireMember(Long memberId) {
        if (memberId == null) {
            throw new SubmissionException(SubmissionErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }

    /** Body 없이 보내도 "모두 비운 제출"로 본다 (질문은 모두 선택 응답) */
    private static SubmissionRequest orEmpty(SubmissionRequest request) {
        return request == null ? new SubmissionRequest(null) : request;
    }
}
