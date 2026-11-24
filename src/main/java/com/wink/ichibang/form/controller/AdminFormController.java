package com.wink.ichibang.form.controller;

import com.wink.ichibang.auth.CurrentMember;
import com.wink.ichibang.form.service.FormService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.wink.ichibang.form.dto.FormResponses.Applicants;
import com.wink.ichibang.form.dto.FormResponses.SubmissionDetail;
import com.wink.ichibang.form.dto.FormResponses.ActivityEnd;
import com.wink.ichibang.form.dto.FormResponses.Completion;
import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts/{postId}")
public class AdminFormController {
    private final FormService forms;
    private final CurrentMember currentMember;

    @GetMapping("/submissions")
    public Applicants applicants(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        currentMember.requireAdminId();
        return forms.applicants(postId, page, size);
    }

    @GetMapping("/submissions/{submissionId}")
    public SubmissionDetail detail(
            @PathVariable Long postId,
            @PathVariable Long submissionId
    ) {
        currentMember.requireAdminId();
        return forms.detail(postId, submissionId);
    }

    @PostMapping("/activity/end")
    public ActivityEnd endActivity(@PathVariable Long postId) {
        currentMember.requireAdminId();
        return forms.endActivity(postId);
    }

    @PatchMapping("/submissions/{submissionId}/complete")
    public Completion complete(
            @PathVariable Long postId,
            @PathVariable Long submissionId
    ) {
        currentMember.requireAdminId();
        return forms.complete(postId, submissionId);
    }

    @GetMapping(value = "/submissions/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(@PathVariable Long postId) {
        currentMember.requireAdminId();
        String csv = forms.exportCsv(postId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=post-" + postId + "-submissions.csv"
                )
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}