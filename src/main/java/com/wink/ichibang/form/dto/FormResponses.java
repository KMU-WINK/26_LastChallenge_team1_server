package com.wink.ichibang.form.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.domain.QuestionType;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** FORM-02~06 요청·응답. 필드 이름과 모양은 기능 명세 v4를 따른다 */
public final class FormResponses {

    private FormResponses() {
    }

    // ===== 공통 =====

    public record OptionView(Long id, String content) {
    }

    // ===== FORM-02 신청서 조회 =====

    public record MyForm(
            Long postId,
            String title,
            PostType type,
            LocalDate startDate,
            LocalDate endDate,
            List<QuestionView> questions,
            MySubmission mySubmission          // 미신청이면 null
    ) {
    }

    /** options는 단답·서술이면 빈 배열 */
    public record QuestionView(
            Long id, String content, String description, QuestionType type, List<OptionView> options
    ) {
    }

    public record MySubmission(Long submissionId, SubmissionStatus status, List<MyAnswer> answers) {
    }

    /**
     * 객관식은 { questionId, optionIds }, 단답·서술은 { questionId, text }.
     * 쓰지 않는 쪽은 null이라 응답에서 빠진다.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MyAnswer(Long questionId, List<Long> optionIds, String text) {
    }

    // ===== FORM-03 제출자 목록 =====

    public record Applicants(
            PostSummary post,
            Counts counts,
            List<ApplicantRow> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record PostSummary(
            Long id, String title, PostType type, PostStatus status, LocalDate startDate, LocalDate endDate
    ) {
    }

    /** status 필터와 상관없이 글 전체 기준 (탭 숫자용) */
    public record Counts(long total, long submitted, long completed) {
    }

    public record ApplicantRow(
            Long submissionId, String name, String studentNo, LocalDate submittedAt, SubmissionStatus status
    ) {
    }

    // ===== FORM-06 제출 상세 =====

    public record SubmissionDetail(
            Long submissionId,
            String name,
            String studentNo,
            SubmissionStatus status,
            LocalDateTime submittedAt,
            List<AnswerDetail> answers
    ) {
    }

    /** 질문 순서대로 전부. 답하지 않은 질문은 selectedOptions: [], text: null */
    public record AnswerDetail(
            Long questionId, String content, QuestionType type, List<OptionView> selectedOptions, String text
    ) {
    }

    // ===== FORM-04 참여 완료 처리 =====

    public record StatusChangeRequest(
            @NotNull(message = "상태 값을 확인해주세요") SubmissionStatus status
    ) {
    }

    /** completedAt은 제출됨으로 되돌리면 null */
    public record StatusChangeResponse(Long submissionId, SubmissionStatus status, LocalDateTime completedAt) {
    }
}
