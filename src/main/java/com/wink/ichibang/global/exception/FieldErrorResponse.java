package com.wink.ichibang.global.exception;

/** fieldErrors 한 칸. 예: { "field": "questions[0].options", "reason": "객관식 질문마다 선택지를 1개 이상 추가해주세요" } */
public record FieldErrorResponse(String field, String reason) {
}
