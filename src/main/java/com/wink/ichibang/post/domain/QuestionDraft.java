package com.wink.ichibang.post.domain;

import java.util.List;

/**
 * 저장할 질문 한 개 (작성·수정 요청을 도메인으로 옮긴 값).
 * 검증은 서비스(QuestionDraftFactory)에서 끝난 상태로 들어온다.
 *
 * @param id 기존 질문이면 ID, 새 질문이면 null
 */
public record QuestionDraft(
        Long id,
        String content,
        String description,
        QuestionType type,
        List<OptionDraft> options
) {
    /** @param id 기존 선택지면 ID, 새 선택지면 null */
    public record OptionDraft(Long id, String content) {
    }
}
