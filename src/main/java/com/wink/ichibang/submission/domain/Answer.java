package com.wink.ichibang.submission.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 답변: 질문 하나에 답 하나 (V1__init.sql의 answer + answer_option).
 * 객관식은 고른 선택지를 answer_option에, 단답·서술은 text에 담는다.
 */
@Entity
@Table(name = "answer", uniqueConstraints = @UniqueConstraint(
        name = "uk_answer_submission_question", columnNames = {"submission_id", "question_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String text;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "answer_option", joinColumns = @JoinColumn(name = "answer_id"))
    @Column(name = "option_id", nullable = false)
    private Set<Long> optionIds = new LinkedHashSet<>();

    static Answer of(Submission submission, AnswerDraft draft) {
        Answer answer = new Answer();
        answer.submission = submission;
        answer.questionId = draft.questionId();
        answer.text = draft.text();
        answer.optionIds.addAll(draft.optionIds());
        return answer;
    }
}
