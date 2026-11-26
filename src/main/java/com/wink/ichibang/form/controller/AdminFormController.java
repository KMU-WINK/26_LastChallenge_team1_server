package com.wink.ichibang.form.controller;

import com.wink.ichibang.auth.CurrentMember;
import com.wink.ichibang.form.dto.FormResponses.Applicants;
import com.wink.ichibang.form.dto.FormResponses.StatusChangeRequest;
import com.wink.ichibang.form.dto.FormResponses.StatusChangeResponse;
import com.wink.ichibang.form.dto.FormResponses.SubmissionDetail;
import com.wink.ichibang.form.service.FormService;
import com.wink.ichibang.form.service.FormService.CsvFile;
import com.wink.ichibang.global.response.ApiResponse;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/** 운영진용 제출자 관리 (COM-01: 운영진만) */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts/{postId}/submissions")
public class AdminFormController {

    private final FormService formService;
    private final CurrentMember currentMember;

    /** FORM-03 제출자 목록. status 없으면 전체 */
    @GetMapping
    public ApiResponse<Applicants> applicants(
            @PathVariable Long postId,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이에요") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~100이에요")
            @Max(value = 100, message = "size는 1~100이에요") int size) {
        currentMember.requireAdminId();
        return ApiResponse.of(formService.applicants(postId, status, page, size));
    }

    /** FORM-06 제출 상세 */
    @GetMapping("/{submissionId}")
    public ApiResponse<SubmissionDetail> detail(@PathVariable Long postId, @PathVariable Long submissionId) {
        currentMember.requireAdminId();
        return ApiResponse.of(formService.detail(postId, submissionId));
    }

    /** FORM-04 참여 완료 처리(COMPLETED)·되돌리기(SUBMITTED) */
    @PatchMapping("/{submissionId}/status")
    public ApiResponse<StatusChangeResponse> changeStatus(@PathVariable Long postId,
                                                          @PathVariable Long submissionId,
                                                          @Valid @RequestBody StatusChangeRequest request) {
        currentMember.requireAdminId();
        return ApiResponse.of(formService.changeStatus(postId, submissionId, request.status()));
    }

    /** FORM-05 CSV 내려받기. 파일명 신청_{제목}_{yyyyMMdd}.csv, UTF-8 BOM 포함 */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export(@PathVariable Long postId) {
        currentMember.requireAdminId();
        CsvFile file = formService.exportCsv(postId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8) // 한글 파일명 (filename*=UTF-8''...)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(file.content().getBytes(StandardCharsets.UTF_8));
    }
}
