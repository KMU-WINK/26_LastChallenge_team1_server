package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.port.SubmissionPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** POST-05 부원 상세 */
public record PostDetailResponse(
        Long id,
        PostType type,
        String title,
        String summary,
        String content,
        String authorName,
        LocalDate createdAt,
        LocalDate startDate,
        LocalDate endDate,
        PostStatus status,
        Integer dDay,
        MySubmissionResponse mySubmission
) {
    /** 등록자는 개인 이름 대신 항상 이 표기 */
    public static final String AUTHOR_NAME = "WINK 운영진";

    /** @param mySubmission 비로그인이거나 미신청이면 null */
    public static PostDetailResponse of(Post post, LocalDate today, SubmissionPort.MySubmission mySubmission) {
        return new PostDetailResponse(
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getSummary(),
                post.getContent(),
                AUTHOR_NAME,
                post.getCreatedAt().toLocalDate(),
                post.getStartDate(),
                post.getEndDate(),
                post.statusOn(today),
                post.dDayOn(today),
                mySubmission == null ? null : MySubmissionResponse.of(mySubmission)
        );
    }

    public record MySubmissionResponse(Long submissionId, String status, LocalDateTime submittedAt) {

        static MySubmissionResponse of(SubmissionPort.MySubmission s) {
            // 명세 예시 "2026-11-20T14:32:00"에 맞춰 초 단위로 자른다
            return new MySubmissionResponse(s.submissionId(), s.status(),
                    s.submittedAt().truncatedTo(ChronoUnit.SECONDS));
        }
    }
}
