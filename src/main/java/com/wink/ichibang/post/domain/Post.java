package com.wink.ichibang.post.domain;

import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@SQLRestriction("deleted_at IS NULL")   // 소프트 삭제된 글은 모든 조회에서 제외 (POST-03)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    /** 서버 시간 기준. 서비스에서도 이 값을 쓴다 */
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 신청 기간은 날짜 단위: 시작일 00:00:00 ~ 마감일 23:59:59 */
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String summary;                 // 한 줄 소개 (선택)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;             // 본문

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PostStatus status;

    @Column(name = "opens_at", nullable = false)
    private LocalDateTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 신청서 질문 (FORM-01). 신청글과 함께 저장·교체된다 */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<Question> questions = new ArrayList<>();

    // ===== 생성 (POST-01) =====

    /** 작성 즉시 게시 상태. 질문은 addQuestions()로 붙인다 */
    public static Post create(String title, String summary, String description,
                              LocalDate opensOn, LocalDate closesOn,
                              Long createdBy, LocalDateTime now) {
        validatePeriod(opensOn, closesOn, now);

        Post post = new Post();
        post.title = title;
        post.summary = normalize(summary);
        post.description = description;
        post.opensAt = opensOn.atStartOfDay();
        post.closesAt = closesOn.atTime(END_OF_DAY);
        post.status = PostStatus.OPEN;
        post.createdBy = createdBy;
        return post;
    }

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

    // ===== 상태 계산 (POST-04 상태 표) =====

    public DisplayStatus displayStatus(LocalDateTime now) {
        if (status == PostStatus.CLOSED || now.isAfter(closesAt)) {
            return DisplayStatus.CLOSED;
        }
        if (now.isBefore(opensAt)) {
            return DisplayStatus.UPCOMING;
        }
        return DisplayStatus.OPEN;
    }

    /** 부원에게 보이는가: 시작일이 지났음 (모집 전 글은 숨김) */
    public boolean isVisible(LocalDateTime now) {
        return !now.isBefore(opensAt);
    }

    /** 지금 신청할 수 있는가 = 모집 중 */
    public boolean isSubmittable(LocalDateTime now) {
        return displayStatus(now) == DisplayStatus.OPEN;
    }

    /** 모집 중일 때만 마감일까지 남은 일수, 마감 당일은 0 */
    public Integer dDay(LocalDateTime now) {
        if (!isSubmittable(now)) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(now.toLocalDate(), getClosesOn());
    }

    public LocalDate getOpensOn() {
        return opensAt.toLocalDate();
    }

    public LocalDate getClosesOn() {
        return closesAt.toLocalDate();
    }

    // ===== 마감·삭제 (POST-06, POST-03) =====

    /** 수동 마감. 마감일은 그대로 둔다 */
    public void close(LocalDateTime now) {
        if (displayStatus(now) == DisplayStatus.CLOSED) {
            throw new PostException(PostErrorCode.POST_ALREADY_CLOSED);
        }
        this.status = PostStatus.CLOSED;
    }

    public void softDelete(LocalDateTime now) {
        this.deletedAt = now;
    }

    // ===== 수정 (POST-02) =====

    /** 마감된 글은 수정 불가 */
    public void requireEditable(LocalDateTime now) {
        if (displayStatus(now) == DisplayStatus.CLOSED) {
            throw new PostException(PostErrorCode.POST_CLOSED);
        }
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    /** 빈 문자열을 보내면 한 줄 소개를 지운다 */
    public void changeSummary(String summary) {
        this.summary = normalize(summary);
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    /**
     * 시작일: 모집 전일 때만 (같은 값을 다시 보낸 것은 무시)
     * 마감일: 오늘 이후로만
     */
    public void changePeriod(LocalDate newOpensOn, LocalDate newClosesOn, LocalDateTime now) {
        boolean opensOnChanged = newOpensOn != null && !newOpensOn.equals(getOpensOn());
        if (opensOnChanged && displayStatus(now) != DisplayStatus.UPCOMING) {
            throw new PostException(PostErrorCode.POST_FIELD_LOCKED);
        }

        LocalDate opensOn = opensOnChanged ? newOpensOn : getOpensOn();
        LocalDate closesOn = (newClosesOn != null) ? newClosesOn : getClosesOn();
        validatePeriod(opensOn, closesOn, now);

        this.opensAt = opensOn.atStartOfDay();
        this.closesAt = closesOn.atTime(END_OF_DAY);
    }

    /** 질문만 바뀐 경우에도 수정 시각을 남긴다 */
    public void touch() {
        this.updatedAt = LocalDateTime.now(KST);
    }

    // ===== 질문 (FORM-01) =====

    /** 받은 순서대로 1부터 순서 번호를 매겨 붙인다 */
    public void addQuestions(List<Question> newQuestions) {
        int orderNo = questions.size() + 1;
        for (Question question : newQuestions) {
            question.assignTo(this, orderNo++);
            questions.add(question);
        }
    }

    /** 질문 전체 삭제 (orphanRemoval로 DB에서도 지워짐) */
    public void clearQuestions() {
        questions.clear();
    }

    // ===== 내부 =====

    private static void validatePeriod(LocalDate opensOn, LocalDate closesOn, LocalDateTime now) {
        if (opensOn.isAfter(closesOn) || closesOn.isBefore(now.toLocalDate())) {
            throw new PostException(PostErrorCode.INVALID_PERIOD);
        }
    }

    private static String normalize(String summary) {
        return (summary == null || summary.isBlank()) ? null : summary.trim();
    }
}
