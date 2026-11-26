package com.wink.ichibang.submission.controller;

import com.wink.ichibang.auth.CurrentMember;
import com.wink.ichibang.global.response.ApiResponse;
import com.wink.ichibang.submission.dto.request.SubmissionRequest;
import com.wink.ichibang.submission.dto.response.SubmissionCreatedResponse;
import com.wink.ichibang.submission.dto.response.SubmissionUpdatedResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부원 신청서 제출·수정·취소 (COM-01: 로그인 필요).
 * 로그인한 부원은 CurrentMember로 꺼낸다 (헤더가 없거나 1 이상의 숫자가 아니면 401).
 */
@RestController
@RequestMapping("/api/posts/{postId}/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CurrentMember currentMember;

    /** SUB-01 신청서 제출 → 201 */
    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionCreatedResponse>> submit(
            @PathVariable Long postId,
            @Valid @RequestBody(required = false) SubmissionRequest request) {
        SubmissionCreatedResponse response =
                submissionService.submit(postId, currentMember.requireId(), orEmpty(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** SUB-05 답변 수정 → 200 */
    @PutMapping("/me")
    public ApiResponse<SubmissionUpdatedResponse> update(
            @PathVariable Long postId,
            @Valid @RequestBody(required = false) SubmissionRequest request) {
        return ApiResponse.of(submissionService.update(postId, currentMember.requireId(), orEmpty(request)));
    }

    /** SUB-03 신청 취소 → 204 */
    @DeleteMapping("/me")
    public ResponseEntity<Void> cancel(@PathVariable Long postId) {
        submissionService.cancel(postId, currentMember.requireId());
        return ResponseEntity.noContent().build();
    }

    /** Body 없이 보내도 "모두 비운 제출"로 본다 (질문은 모두 선택 응답) */
    private static SubmissionRequest orEmpty(SubmissionRequest request) {
        return request == null ? new SubmissionRequest(null) : request;
    }
}
