package com.wink.ichibang.post.domain;

import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 신청글 (POST-01~07).
 *
 * 상태 규칙
 * - DB status는 운영진의 수동 마감(POST-06)만 기록한다.
 * - 화면에 내보내는 상태는 {@link #statusOn(LocalDate)}로 계산한다.
 *   수동 마감했거나 종료일이 지났으면 CLOSED, 아니면 OPEN.
 */
@Entity
@Table(name = "post")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // MySQL ENUM 대신 VARCHAR로 검증 (ddl-auto: validate 대비)
    @Column(nullable = false, length = 10)
    private PostType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // MySQL ENUM 대신 VARCHAR로 검증 (ddl-auto: validate 대비)
    @Column(nullable = false, length = 10)
    private PostStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<Question> questions = new ArrayList<>();

    // ===== 생성 (POST-01) =====

    public static Post create(PostType type, String title, String summary, String content,
                              LocalDate startDate, LocalDate endDate, Long createdBy,
                              List<QuestionDraft> drafts) {
        validatePeriod(startDate, endDate);
        Post post = new Post();
        post.type = type;
        post.title = title;
        post.summary = summary;
        post.content = content;
        post.startDate = startDate;
        post.endDate = endDate;
        post.createdBy = createdBy;
        post.status = PostStatus.OPEN;
        for (int i = 0; i < drafts.size(); i++) {
            post.questions.add(Question.create(post, i + 1, drafts.get(i)));
        }
        return post;
    }

    // ===== 상태 계산 =====

    /** 화면에 내보내는 상태. 수동 마감이거나 종료일이 지나면 CLOSED */
    public PostStatus statusOn(LocalDate today) {
        if (status == PostStatus.CLOSED || today.isAfter(endDate)) {
            return PostStatus.CLOSED;
        }
        return PostStatus.OPEN;
    }

    /** 모집 중일 때만 종료일까지 남은 일수, 마감이면 null */
    public Integer dDayOn(LocalDate today) {
        if (statusOn(today) == PostStatus.CLOSED) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(today, endDate);
    }

    // ===== 수정 (POST-02) =====

    /** 마감된 글이면 409 POST_CLOSED */
    public void requireOpen(LocalDate today) {
        if (statusOn(today) == PostStatus.CLOSED) {
            throw new PostException(PostErrorCode.POST_CLOSED);
        }
    }

    public void updateBasicInfo(String title, String summary, String content,
                                LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** 요청한 질문 목록이 지금 저장된 질문·선택지(순서 포함)와 다른지 */
    public boolean isFormChanged(List<QuestionDraft> drafts) {
        if (drafts.size() != questions.size()) {
            return true;
        }
        for (int i = 0; i < drafts.size(); i++) {
            if (!questions.get(i).sameAs(drafts.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 질문 병합. id가 있으면 유지·수정, 없으면 추가, 요청에서 빠지면 삭제.
     * 배열 순서가 곧 표시 순서다.
     */
    public void mergeQuestions(List<QuestionDraft> drafts) {
        Map<Long, Question> existing = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (int i = 0; i < drafts.size(); i++) {
            Long id = drafts.get(i).id();
            if (id == null) {
                continue;
            }
            if (!existing.containsKey(id) || !keepIds.add(id)) {
                throw PostException.field("questions[" + i + "].id", "이 신청글의 질문이 아니에요");
            }
        }

        questions.removeIf(q -> !keepIds.contains(q.getId()));

        for (int i = 0; i < drafts.size(); i++) {
            QuestionDraft draft = drafts.get(i);
            if (draft.id() == null) {
                questions.add(Question.create(this, i + 1, draft));
            } else {
                existing.get(draft.id()).update(i + 1, draft, i);
            }
        }
        questions.sort(Comparator.comparingInt(Question::getOrderNo));
    }

    // ===== 마감 (POST-06) =====

    /** OPEN → CLOSED. 이미 마감(수동·자동)이면 409 POST_CLOSED */
    public void close(LocalDate today) {
        requireOpen(today);
        this.status = PostStatus.CLOSED;
    }

    // ===== 삭제 (POST-03) =====

    public void softDelete() {
        this.deletedAt = LocalDateTime.now(KST);
    }

    // ===== 공통 =====

    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new PostException(PostErrorCode.INVALID_PERIOD);
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(KST);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now(KST);
    }
}
