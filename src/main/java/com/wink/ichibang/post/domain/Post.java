package com.wink.ichibang.post.domain;

import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "post")
@SQLRestriction("deleted_at IS NULL")   // 소프트 삭제된 글은 모든 조회에서 제외 (POST-03)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    /** 서버 시간 기준 (명세서 2장). 서비스에서도 이 값을 쓴다 */
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 100)
    private String summary;                 // 목록에서 제목 아래 보이는 한 줄 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PostStatus status;

    @Column(name = "opens_at", nullable = false)
    private LocalDateTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Post(String title, String summary,
                 LocalDateTime opensAt, LocalDateTime closesAt, Long createdBy) {
        validatePeriod(opensAt, closesAt);
        this.title = title;
        this.summary = summary;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.createdBy = createdBy;
        this.status = PostStatus.DRAFT;
    }

    /** POST-01: 등록하면 항상 임시 저장 상태 */
    public static Post createDraft(String title, String summary,
                                   LocalDateTime opensAt, LocalDateTime closesAt, Long createdBy) {
        return new Post(title, summary, opensAt, closesAt, createdBy);
    }

    // ===== 생성·수정 시각 자동 기록 =====

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now(KST);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now(KST);
    }

    // ===== 상태 계산 (명세서 2장 표) =====

    /** 게시됐어도 마감 일시가 지나면 CLOSED로 본다 (자동 마감) */
    public PostStatus effectiveStatus(LocalDateTime now) {
        if (status == PostStatus.OPEN && now.isAfter(closesAt)) {
            return PostStatus.CLOSED;
        }
        return status;
    }

    /** 부원에게 보이는가: 게시됐고 시작 일시가 지남 */
    public boolean isVisible(LocalDateTime now) {
        return status != PostStatus.DRAFT && !now.isBefore(opensAt);
    }

    /** 지금 제출·취소가 가능한가 (BE-2 제출 검증에서 사용) */
    public boolean isSubmittable(LocalDateTime now) {
        return isVisible(now) && effectiveStatus(now) == PostStatus.OPEN;
    }

    /** 신청 중일 때만 마감까지 남은 일수, 마감 당일은 0 */
    public Integer dDay(LocalDateTime now) {
        if (!isSubmittable(now)) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(now.toLocalDate(), closesAt.toLocalDate());
    }

    // ===== 상태 변경 =====

    /** POST-06: 임시 저장 → 게시 */
    public void publish(LocalDateTime now) {
        if (status != PostStatus.DRAFT) {
            throw new PostException(PostErrorCode.POST_NOT_DRAFT);
        }
        if (!closesAt.isAfter(now)) {
            throw new PostException(PostErrorCode.POST_PERIOD_PASSED);
        }
        this.status = PostStatus.OPEN;
        this.publishedAt = now;
    }

    /** POST-06 (제안): 수동 마감. closesAt은 그대로 둔다 */
    public void close(LocalDateTime now) {
        PostStatus current = effectiveStatus(now);
        if (current == PostStatus.DRAFT) {
            throw new PostException(PostErrorCode.POST_NOT_PUBLISHED);
        }
        if (current == PostStatus.CLOSED) {
            throw new PostException(PostErrorCode.POST_ALREADY_CLOSED);
        }
        this.status = PostStatus.CLOSED;
    }

    /** POST-03: 소프트 삭제 */
    public void softDelete(LocalDateTime now) {
        this.deletedAt = now;
    }

    // ===== 수정 (POST-02, 명세서 P-8 표) =====

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeSummary(String summary) {
        this.summary = summary;
    }

    /**
     * opensAt: 임시 저장 또는 공개 대기일 때만
     * closesAt: 마감 전까지, 게시된 글은 현재 이후로만
     */
    public void changePeriod(LocalDateTime newOpensAt, LocalDateTime newClosesAt, LocalDateTime now) {
        PostStatus current = effectiveStatus(now);
        boolean beforeOpen = current != PostStatus.CLOSED
                && (status == PostStatus.DRAFT || now.isBefore(opensAt));

        if (newOpensAt != null && !beforeOpen) {
            throw new PostException(PostErrorCode.POST_FIELD_LOCKED);
        }
        if (newClosesAt != null) {
            if (current == PostStatus.CLOSED) {
                throw new PostException(PostErrorCode.POST_FIELD_LOCKED);
            }
            if (status != PostStatus.DRAFT && !newClosesAt.isAfter(now)) {
                throw new PostException(PostErrorCode.INVALID_PERIOD);
            }
        }

        LocalDateTime opens = (newOpensAt != null) ? newOpensAt : this.opensAt;
        LocalDateTime closes = (newClosesAt != null) ? newClosesAt : this.closesAt;
        validatePeriod(opens, closes);
        this.opensAt = opens;
        this.closesAt = closes;
    }

    private void requireDraft() {
        if (status != PostStatus.DRAFT) {
            throw new PostException(PostErrorCode.POST_FIELD_LOCKED);
        }
    }

    private static void validatePeriod(LocalDateTime opensAt, LocalDateTime closesAt) {
        if (!opensAt.isBefore(closesAt)) {
            throw new PostException(PostErrorCode.INVALID_PERIOD);
        }
    }
}
