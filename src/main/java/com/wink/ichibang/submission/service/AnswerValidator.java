package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.domain.QuestionOption;
import com.wink.ichibang.post.domain.QuestionType;
import com.wink.ichibang.submission.domain.AnswerDraft;
import com.wink.ichibang.submission.dto.request.SubmissionRequest.AnswerRequest;
import com.wink.ichibang.global.exception.BusinessException;
import com.wink.ichibang.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 요청한 답변이 이 신청글의 질문에 맞는지 검증해 저장할 값으로 바꾼다 (SUB-01, SUB-05 → INVALID_ANSWER).
 * 질문은 모두 선택 응답이라, 비어 있는 답변은 오류가 아니라 "답변 없음"으로 보고 건너뛴다.
 */
@Component
public class AnswerValidator {

    // TODO(확인 필요): 단답·서술 글자 수 제한은 명세에서 미정. 서버·화면이 같은 값을 써야 한다
    static final int SHORT_MAX_LENGTH = 100;
    static final int LONG_MAX_LENGTH = 1000;

    public List<AnswerDraft> validate(Post post, List<AnswerRequest> requests) {
        Map<Long, Question> questions = post.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        Set<Long> answeredQuestionIds = new HashSet<>();
        List<AnswerDraft> drafts = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            AnswerRequest request = requests.get(i);
            String field = "answers[" + i + "]";

            Question question = questions.get(request.questionId());
            if (question == null) {
                throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".questionId", "이 신청글의 질문이 아니에요");
            }
            if (!answeredQuestionIds.add(question.getId())) {
                throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".questionId", "같은 질문에 답변이 두 번 있어요");
            }

            AnswerDraft draft = question.getType().isChoice()
                    ? validateChoice(question, request, field)
                    : validateText(question, request, field);
            if (draft != null) {
                drafts.add(draft);
            }
        }
        return drafts;
    }

    /** 객관식: text는 보낼 수 없고, 선택지는 이 질문의 것이어야 한다. 비면 답변 없음 */
    private AnswerDraft validateChoice(Question question, AnswerRequest request, String field) {
        if (request.text() != null && !request.text().isBlank()) {
            throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".text", "객관식 질문에는 글로 답할 수 없어요");
        }
        List<Long> optionIds = request.optionIds() == null ? List.of() : request.optionIds();
        if (optionIds.isEmpty()) {
            return null;
        }
        if (question.getType() == QuestionType.SINGLE && optionIds.size() > 1) {
            throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".optionIds", "하나만 선택할 수 있어요");
        }

        Set<Long> validIds = question.getOptions().stream()
                .map(QuestionOption::getId)
                .collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>();
        for (Long optionId : optionIds) {
            if (optionId == null || !validIds.contains(optionId)) {
                throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".optionIds", "이 질문의 선택지가 아니에요");
            }
            if (!seen.add(optionId)) {
                throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".optionIds", "같은 선택지를 두 번 골랐어요");
            }
        }
        return new AnswerDraft(question.getId(), List.copyOf(optionIds), null);
    }

    /** 단답·서술: 선택지는 보낼 수 없고, 글자 수 제한 안이어야 한다. 공백뿐이면 답변 없음 */
    private AnswerDraft validateText(Question question, AnswerRequest request, String field) {
        if (request.optionIds() != null && !request.optionIds().isEmpty()) {
            throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".optionIds", "글로 답하는 질문에는 선택지를 보낼 수 없어요");
        }
        String text = request.text();
        if (text == null || text.isBlank()) {
            return null;
        }
        int max = question.getType() == QuestionType.SHORT ? SHORT_MAX_LENGTH : LONG_MAX_LENGTH;
        if (text.length() > max) {
            throw BusinessException.field(ErrorCode.INVALID_ANSWER, field + ".text", "답변은 " + max + "자까지 쓸 수 있어요");
        }
        return new AnswerDraft(question.getId(), List.of(), text);
    }
}
