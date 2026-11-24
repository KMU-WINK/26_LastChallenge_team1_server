package com.wink.ichibang.submission.dto.response;

import com.wink.ichibang.submission.domain.Submission;
import com.wink.ichibang.submission.domain.SubmissionStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** SUB-01 응답 */
public record SubmissionCreatedResponse(Long submissionId, SubmissionStatus status, LocalDateTime submittedAt) {

    public static SubmissionCreatedResponse of(Submission submission) {
        // 명세 예시 "2026-11-20T14:32:00"에 맞춰 초 단위로 자른다
        return new SubmissionCreatedResponse(submission.getId(), submission.getStatus(),
                submission.getSubmittedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
