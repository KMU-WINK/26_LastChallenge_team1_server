package com.wink.ichibang.submission.dto.response;

import com.wink.ichibang.submission.domain.Submission;
import com.wink.ichibang.submission.domain.SubmissionStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** SUB-05 응답 */
public record SubmissionUpdatedResponse(Long submissionId, SubmissionStatus status, LocalDateTime updatedAt) {

    public static SubmissionUpdatedResponse of(Submission submission) {
        return new SubmissionUpdatedResponse(submission.getId(), submission.getStatus(),
                submission.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
