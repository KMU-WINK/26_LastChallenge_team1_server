package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.global.time.TimeZones;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.domain.QuestionDraft;
import com.wink.ichibang.post.domain.QuestionDraft.OptionDraft;
import com.wink.ichibang.post.domain.QuestionType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

/** DB 없이 ID가 채워진 Post를 만든다 (질문 1~4번, 선택지 11·12 / 21·22) */
final class SubmissionTestFixtures {

    static final long SINGLE_Q = 1L;   // 선택지 11, 12
    static final long MULTI_Q = 2L;    // 선택지 21, 22
    static final long SHORT_Q = 3L;
    static final long LONG_Q = 4L;

    private SubmissionTestFixtures() {
    }

    /** 오늘 시작해 내일 끝나는 모집 중인 글 */
    static Post openPost() {
        LocalDate today = LocalDate.now(TimeZones.KST);
        return post(today, today.plusDays(1));
    }

    static Post post(LocalDate startDate, LocalDate endDate) {
        Post post = Post.create(PostType.STUDY, "Spring Boot 챌린지", null, "본문",
                startDate, endDate, 1L, List.of(
                        draft("학년", QuestionType.SINGLE, "1학년", "2학년"),
                        draft("시간대", QuestionType.MULTI, "평일 저녁", "주말 오후"),
                        draft("GitHub", QuestionType.SHORT),
                        draft("지원 동기", QuestionType.LONG)));
        ReflectionTestUtils.setField(post, "id", 100L);

        long[][] optionIds = {{11, 12}, {21, 22}, {}, {}};
        for (int i = 0; i < post.getQuestions().size(); i++) {
            var question = post.getQuestions().get(i);
            ReflectionTestUtils.setField(question, "id", (long) (i + 1));
            for (int j = 0; j < question.getOptions().size(); j++) {
                ReflectionTestUtils.setField(question.getOptions().get(j), "id", optionIds[i][j]);
            }
        }
        return post;
    }

    private static QuestionDraft draft(String content, QuestionType type, String... options) {
        List<OptionDraft> optionDrafts = java.util.Arrays.stream(options)
                .map(o -> new OptionDraft(null, o)).toList();
        return new QuestionDraft(null, content, null, type, optionDrafts);
    }
}
