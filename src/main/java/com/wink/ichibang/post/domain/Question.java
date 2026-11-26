package com.wink.ichibang.post.domain;

import com.wink.ichibang.global.exception.BusinessException;
import com.wink.ichibang.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 신청서 질문 (FORM-01). 신청글에 딸려 저장·수정·삭제된다 */
@Entity
@Table(name = "question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // MySQL ENUM 대신 VARCHAR로 검증 (ddl-auto: validate 대비)
    @Column(nullable = false, length = 10)
    private QuestionType type;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<QuestionOption> options = new ArrayList<>();

    static Question create(Post post, int orderNo, QuestionDraft draft) {
        Question question = new Question();
        question.post = post;
        question.orderNo = orderNo;
        question.content = draft.content();
        question.description = draft.description();
        question.type = draft.type();
        List<QuestionDraft.OptionDraft> optionDrafts = draft.options();
        for (int i = 0; i < optionDrafts.size(); i++) {
            question.options.add(QuestionOption.create(question, i + 1, optionDrafts.get(i).content()));
        }
        return question;
    }

    /**
     * 기존 질문 수정 + 선택지 병합.
     * @param index 요청 배열에서의 위치 (오류 메시지의 필드 경로용)
     */
    void update(int orderNo, QuestionDraft draft, int index) {
        this.orderNo = orderNo;
        this.content = draft.content();
        this.description = draft.description();
        this.type = draft.type();
        mergeOptions(draft.options(), index);
    }

    private void mergeOptions(List<QuestionDraft.OptionDraft> drafts, int questionIndex) {
        Map<Long, QuestionOption> existing = options.stream()
                .collect(Collectors.toMap(QuestionOption::getId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (int i = 0; i < drafts.size(); i++) {
            Long id = drafts.get(i).id();
            if (id == null) {
                continue;
            }
            if (!existing.containsKey(id) || !keepIds.add(id)) {
                throw BusinessException.field(ErrorCode.VALIDATION_FAILED,
                        "questions[" + questionIndex + "].options[" + i + "].id", "이 질문의 선택지가 아니에요");
            }
        }

        options.removeIf(o -> !keepIds.contains(o.getId()));

        for (int i = 0; i < drafts.size(); i++) {
            QuestionDraft.OptionDraft draft = drafts.get(i);
            if (draft.id() == null) {
                options.add(QuestionOption.create(this, i + 1, draft.content()));
            } else {
                existing.get(draft.id()).update(i + 1, draft.content());
            }
        }
        options.sort(Comparator.comparingInt(QuestionOption::getOrderNo));
    }

    /** 요청 값과 내용·순서가 같은지 (POST-02: 제출이 있을 때 질문 변경 여부 판단) */
    boolean sameAs(QuestionDraft draft) {
        if (!Objects.equals(id, draft.id())
                || !Objects.equals(content, draft.content())
                || !Objects.equals(description, draft.description())
                || type != draft.type()
                || options.size() != draft.options().size()) {
            return false;
        }
        for (int i = 0; i < options.size(); i++) {
            QuestionOption option = options.get(i);
            QuestionDraft.OptionDraft optionDraft = draft.options().get(i);
            if (!Objects.equals(option.getId(), optionDraft.id())
                    || !Objects.equals(option.getContent(), optionDraft.content())) {
                return false;
            }
        }
        return true;
    }
}
