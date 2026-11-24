package com.wink.ichibang.form.dto;

import com.wink.ichibang.post.domain.QuestionType;
import java.time.LocalDateTime;
import java.util.List;

public final class FormResponses {
    private FormResponses() {}

    public record AnswerView(
            Long questionId, Long optionId, String optionContent, String text
    ) {}

    public record QuestionView(
            Long id, String content, String description,
            QuestionType type, List<OptionView> options
    ) {}

    public record OptionView(Long id, String content) {}

    public record MyForm(
            Long postId, String title, boolean canSubmit, boolean canEdit,
            Long submissionId, String status, LocalDateTime submittedAt,
            List<QuestionView> questions, List<AnswerView> answers
    ) {}

    public record Applicant(
            Long submissionId, Long memberId, String name, String studentNo,
            String status, LocalDateTime submittedAt,
            LocalDateTime completedAt, List<AnswerView> answers
    ) {}

    public record Applicants(
            long applicantCount, long completedCount, long submittedCount,
            List<Applicant> content, int page, int size,
            long totalElements, int totalPages
    ) {}

    public record SubmissionDetail(
            Long postId, String postTitle, Applicant applicant,
            List<QuestionView> questions
    ) {}

    public record Completion(
            Long submissionId, String status, LocalDateTime completedAt
    ) {}

    public record ActivityEnd(
            Long postId, LocalDateTime activityEndedAt
    ) {}
}