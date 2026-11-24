package com.wink.ichibang.post.domain;

/** 신청서 질문 유형 (FORM-01) */
public enum QuestionType {
    SINGLE,  // 객관식 단일 선택 (라디오)
    MULTI,   // 객관식 복수 선택 (체크박스)
    SHORT,   // 단답 (한 줄)
    LONG;    // 서술 (여러 줄)

    public boolean isChoice() {
        return this == SINGLE || this == MULTI;
    }
}
