package com.wink.ichibang.post.domain;

import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 신청서 질문 (FORM-01) */
@Entity
@Table(name = "question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 20;
    private static final int MAX_OPTION_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<QuestionOption> options = new ArrayList<>();

    /**
     * 객관식: 선택지 2~20개, 각 100자 이하, 빈 값·중복 불가
     * 단답형: 선택지를 보내면 오류
     */
    public static Question create(String title, QuestionType type, List<String> optionContents) {
        List<String> contents = (optionContents == null) ? List.of() : optionContents;

        if (type == QuestionType.SHORT_ANSWER && !contents.isEmpty()) {
            throw new PostException(PostErrorCode.INVALID_OPTIONS);
        }
        if (type == QuestionType.MULTIPLE_CHOICE) {
            validateOptions(contents);
        }

        Question question = new Question();
        question.title = title.trim();
        question.type = type;

        int orderNo = 1;
        for (String content : contents) {
            question.options.add(QuestionOption.of(question, orderNo++, content.trim()));
        }
        return question;
    }

    /** Post.addQuestions()에서만 호출 */
    void assignTo(Post post, int orderNo) {
        this.post = post;
        this.orderNo = orderNo;
    }

    private static void validateOptions(List<String> contents) {
        if (contents.size() < MIN_OPTIONS || contents.size() > MAX_OPTIONS) {
            throw new PostException(PostErrorCode.INVALID_OPTIONS);
        }
        Set<String> seen = new HashSet<>();
        for (String content : contents) {
            if (content == null || content.isBlank()) {
                throw new PostException(PostErrorCode.INVALID_OPTIONS);
            }
            String trimmed = content.trim();
            if (trimmed.length() > MAX_OPTION_LENGTH || !seen.add(trimmed)) {
                throw new PostException(PostErrorCode.INVALID_OPTIONS);
            }
        }
    }
}
