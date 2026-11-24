package com.wink.ichibang.post.service;

import com.wink.ichibang.post.domain.QuestionDraft;
import com.wink.ichibang.post.domain.QuestionDraft.OptionDraft;
import com.wink.ichibang.post.domain.QuestionType;
import com.wink.ichibang.post.dto.request.PostCreateRequest.QuestionCreateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest.OptionUpdateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest.QuestionUpdateRequest;
import com.wink.ichibang.post.exception.FieldErrorResponse;
import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;

import java.util.ArrayList;
import java.util.List;

/**
 * 요청 DTO의 질문 목록 → 도메인 QuestionDraft 목록 (FORM-01 규칙 검사 포함).
 *
 * - 질문은 0개도 된다.
 * - SINGLE·MULTI는 선택지 1개 이상. 아니면 VALIDATION_FAILED (fieldErrors: questions[i].options)
 * - SHORT·LONG에 선택지가 와도 버린다.
 * - 빈 설명은 null로 저장한다.
 */
final class QuestionDraftMapper {

    static final String OPTIONS_REQUIRED = "객관식 질문마다 선택지를 1개 이상 추가해주세요";

    private QuestionDraftMapper() {
    }

    static List<QuestionDraft> fromCreate(List<QuestionCreateRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        List<QuestionDraft> drafts = new ArrayList<>();
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            QuestionCreateRequest r = requests.get(i);
            List<OptionDraft> options = r.options() == null ? List.of()
                    : r.options().stream().map(content -> new OptionDraft(null, content)).toList();
            drafts.add(toDraft(null, r.content(), r.description(), r.type(), options, i, errors));
        }
        throwIfAny(errors);
        return drafts;
    }

    static List<QuestionDraft> fromUpdate(List<QuestionUpdateRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        List<QuestionDraft> drafts = new ArrayList<>();
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            QuestionUpdateRequest r = requests.get(i);
            List<OptionDraft> options = r.options() == null ? List.of()
                    : r.options().stream().map(QuestionDraftMapper::toOptionDraft).toList();
            drafts.add(toDraft(r.id(), r.content(), r.description(), r.type(), options, i, errors));
        }
        throwIfAny(errors);
        return drafts;
    }

    private static OptionDraft toOptionDraft(OptionUpdateRequest r) {
        return new OptionDraft(r.id(), r.content());
    }

    private static QuestionDraft toDraft(Long id, String content, String description, QuestionType type,
                                         List<OptionDraft> options, int index, List<FieldErrorResponse> errors) {
        if (type.isChoice() && options.isEmpty()) {
            errors.add(new FieldErrorResponse("questions[" + index + "].options", OPTIONS_REQUIRED));
        }
        List<OptionDraft> kept = type.isChoice() ? options : List.of();
        return new QuestionDraft(id, content, blankToNull(description), type, kept);
    }

    private static void throwIfAny(List<FieldErrorResponse> errors) {
        if (!errors.isEmpty()) {
            throw new PostException(PostErrorCode.VALIDATION_FAILED, errors);
        }
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
