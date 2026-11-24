package com.wink.ichibang.post.dto.response;

import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.domain.QuestionOption;
import com.wink.ichibang.post.domain.QuestionType;

import java.util.List;

/** 신청서 질문 하나 (FORM-01). 단답형이면 options는 빈 배열 */
public record QuestionResponse(
        Long questionId,
        int orderNo,
        String title,
        QuestionType type,
        List<OptionResponse> options
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getOrderNo(),
                question.getTitle(),
                question.getType(),
                question.getOptions().stream().map(OptionResponse::from).toList()
        );
    }

    public record OptionResponse(Long optionId, int orderNo, String content) {

        static OptionResponse from(QuestionOption option) {
            return new OptionResponse(option.getId(), option.getOrderNo(), option.getContent());
        }
    }
}
