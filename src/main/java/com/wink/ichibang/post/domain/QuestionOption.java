package com.wink.ichibang.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 객관식 선택지. 명세의 option 테이블 (option은 MySQL 예약어라 question_option으로 이름 변경) */
@Entity
@Table(name = "question_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(nullable = false, length = 200)
    private String content;

    static QuestionOption create(Question question, int orderNo, String content) {
        QuestionOption option = new QuestionOption();
        option.question = question;
        option.orderNo = orderNo;
        option.content = content;
        return option;
    }

    void update(int orderNo, String content) {
        this.orderNo = orderNo;
        this.content = content;
    }
}
