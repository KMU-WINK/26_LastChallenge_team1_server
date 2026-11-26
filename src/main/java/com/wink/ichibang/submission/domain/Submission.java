package com.wink.ichibang.submission.domain;

import com.wink.ichibang.global.exception.ErrorCode;
import com.wink.ichibang.global.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 제출: 부원 1명이 신청글 1건에 낸 신청서 1개 (SUB-01, 03, 05).
 * 신청글당 1인 1제출이고, 취소하면 행(과 답변)을 삭제한다.
 * post·member는 다른 도메인이라 ID만 들고 있다.
 */
@Entity
@Table(name = "submission", uniqueConstraints = @UniqueConstraint(
        name = "uk_submission_post_member", columnNames = {"post_id", "member_id"}))
@Getter
@DynamicUpdate // 바꾼 컬럼만 UPDATE. 읽어 둔 status·completed_at으로 운영진의 참여 완료 처리를 덮어쓰지 않게 한다
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // MySQL ENUM 대신 VARCHAR로 검증 (ddl-auto: validate 대비)
    @Column(nullable = false, length = 10)
    private SubmissionStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100) // 제출 여러 건의 답변을 읽을 때 건마다 쿼리를 날리지 않는다 (CSV)
    private List<Answer> answers = new ArrayList<>();

    /** 신규 제출 (SUB-01). 상태는 SUBMITTED */
    public static Submission create(Long postId, Long memberId, List<AnswerDraft> drafts, LocalDateTime now) {
        Submission submission = new Submission();
        submission.postId = postId;
        submission.memberId = memberId;
        submission.status = SubmissionStatus.SUBMITTED;
        submission.submittedAt = now;
        submission.updatedAt = now;
        drafts.forEach(draft -> submission.answers.add(Answer.of(submission, draft)));
        return submission;
    }

    /** 참여 완료된 제출은 부원이 수정·취소할 수 없다 → 409 SUBMISSION_COMPLETED */
    public void requireChangeable() {
        if (status == SubmissionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SUBMISSION_COMPLETED);
        }
    }

    /** 참여 완료 처리 (FORM-04). 이미 완료된 제출이면 409 INVALID_STATUS_TRANSITION */
    public void complete(LocalDateTime now) {
        if (status == SubmissionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = SubmissionStatus.COMPLETED;
        this.completedAt = now;
    }

    /** 참여 완료를 취소하고 제출됨으로 되돌린다 (FORM-04). 이미 제출됨이면 409 INVALID_STATUS_TRANSITION */
    public void revert() {
        if (status == SubmissionStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = SubmissionStatus.SUBMITTED;
        this.completedAt = null;
    }

    /** 기존 답변을 모두 지운다. 새 답변을 붙이기 전에 flush해야 한다 (answer 유니크 제약 때문) */
    public void clearAnswers() {
        answers.clear();
    }

    /** 새 답변으로 교체 (SUB-05). 제출 일시는 그대로 두고 수정 일시만 갱신 */
    public void addAnswers(List<AnswerDraft> drafts, LocalDateTime now) {
        drafts.forEach(draft -> answers.add(Answer.of(this, draft)));
        this.updatedAt = now;
    }
}
