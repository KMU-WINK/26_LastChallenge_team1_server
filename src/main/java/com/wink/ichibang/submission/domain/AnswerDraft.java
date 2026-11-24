package com.wink.ichibang.submission.domain;

import java.util.List;

/**
 * 저장할 답변 한 개 (검증이 끝난 값). 답하지 않은 질문은 만들지 않는다.
 *
 * @param optionIds 객관식이면 고른 선택지 ID(1개 이상), 단답·서술이면 빈 목록
 * @param text      단답·서술이면 입력한 글, 객관식이면 null
 */
public record AnswerDraft(Long questionId, List<Long> optionIds, String text) {
}
