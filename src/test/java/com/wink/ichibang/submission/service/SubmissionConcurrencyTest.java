package com.wink.ichibang.submission.service;

import com.wink.ichibang.form.service.FormService;
import com.wink.ichibang.global.time.TimeZones;
import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.domain.QuestionDraft;
import com.wink.ichibang.post.domain.QuestionType;
import com.wink.ichibang.post.repository.PostRepository;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import com.wink.ichibang.submission.dto.request.SubmissionRequest;
import com.wink.ichibang.submission.dto.request.SubmissionRequest.AnswerRequest;
import com.wink.ichibang.global.exception.ErrorCode;
import com.wink.ichibang.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 신청서를 동시에 건드리는 요청이 500 없이 한 줄로 처리되는지 확인한다 (SUB-03, 05 + FORM-04).
 *
 * 유니크 제약·외래키·행 잠금(SELECT ... FOR UPDATE)은 MySQL에서만 그대로 재현되므로 실제 MySQL을 쓴다.
 * 개발 DB(ichibang)를 건드리지 않도록 전용 스키마 ichibang_test를 자동으로 만들어 쓰고,
 * 계정·비밀번호는 로컬 설정(application-local.yml)을 따른다. 트랜잭션을 걸지 않는 이유는
 * 요청마다 별도 트랜잭션으로 커밋되어야 경합이 일어나기 때문이다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/ichibang_test"
                + "?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class SubmissionConcurrencyTest {

    private static final long MEMBER_ID = 10L;

    @Autowired
    SubmissionService submissionService;
    @Autowired
    FormService formService;
    @Autowired
    PostRepository postRepository;
    @Autowired
    JdbcTemplate jdbc;

    Long postId;
    Long questionId;

    @BeforeEach
    void setUp() {
        // 자식 테이블부터 비운다 (외래키 순서)
        for (String table : List.of("answer_option", "answer", "submission", "question_option", "question", "post")) {
            jdbc.update("DELETE FROM " + table);
        }
        LocalDate today = LocalDate.now(TimeZones.KST);
        Post post = postRepository.save(Post.create(PostType.STUDY, "동시성 점검", null, "본문",
                today.minusDays(1), today.plusDays(7), 1L,
                List.of(new QuestionDraft(null, "단답", null, QuestionType.SHORT, List.of()))));
        postId = post.getId();
        questionId = post.getQuestions().get(0).getId();
    }

    private SubmissionRequest answer(String text) {
        return new SubmissionRequest(List.of(new AnswerRequest(questionId, null, text)));
    }

    private Long submit(long memberId) {
        return submissionService.submit(postId, memberId, answer("처음")).submissionId();
    }

    /** 모든 작업을 같은 순간에 시작시키고 결과(값 또는 예외)를 순서대로 돌려준다 */
    private List<Object> runTogether(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return task.call();
                    } catch (Throwable e) {
                        return e;
                    }
                }));
            }
            ready.await();
            go.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean isNotFound(Object result) {
        return result instanceof BusinessException e
                && e.getErrorCode() == ErrorCode.SUBMISSION_NOT_FOUND;
    }

    private static boolean isDuplicate(Object result) {
        return result instanceof BusinessException e
                && e.getErrorCode() == ErrorCode.DUPLICATE_SUBMISSION;
    }

    private static boolean isCompleted(Object result) {
        return result instanceof BusinessException e
                && e.getErrorCode() == ErrorCode.SUBMISSION_COMPLETED;
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    @Test
    void 같은_부원이_동시에_제출하면_하나만_저장되고_나머지는_중복으로_거절된다() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> submissionService.submit(postId, MEMBER_ID, answer("동시")));
        }
        List<Object> results = runTogether(tasks);

        long saved = results.stream().filter(r -> !(r instanceof Throwable)).count();
        long duplicates = results.stream().filter(SubmissionConcurrencyTest::isDuplicate).count();
        assertThat(saved).isEqualTo(1);
        assertThat(duplicates).as("나머지 결과: %s", results).isEqualTo(7);
        assertThat(count("SELECT COUNT(*) FROM submission WHERE post_id = ?", postId)).isEqualTo(1);
    }

    @Test
    void 같은_부원이_동시에_수정해도_모두_성공하고_답변은_하나만_남는다() throws Exception {
        submit(MEMBER_ID);

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String text = "동시" + i;
            tasks.add(() -> submissionService.update(postId, MEMBER_ID, answer(text)));
        }
        List<Object> results = runTogether(tasks);

        assertThat(results).allSatisfy(r -> assertThat(r).isNotInstanceOf(Throwable.class));
        assertThat(count("SELECT COUNT(*) FROM answer a JOIN submission s ON s.id = a.submission_id "
                + "WHERE s.post_id = ?", postId)).isEqualTo(1);
    }

    @Test
    void 수정과_취소가_동시에_와도_오류_없이_정리된다() throws Exception {
        for (int round = 0; round < 5; round++) {
            submit(MEMBER_ID);

            List<Object> results = runTogether(List.of(
                    () -> submissionService.update(postId, MEMBER_ID, answer("수정")),
                    () -> {
                        submissionService.cancel(postId, MEMBER_ID);
                        return "취소";
                    }));

            // 취소는 항상 성공, 수정은 먼저 끝나 성공하거나 취소가 먼저여서 "신청 내역 없음"
            assertThat(results.get(1)).isEqualTo("취소");
            Object update = results.get(0);
            assertThat(update instanceof Throwable ? isNotFound(update) : true)
                    .as("수정 결과: %s", update).isTrue();

            assertThat(count("SELECT COUNT(*) FROM submission WHERE post_id = ?", postId)).isZero();
            assertThat(count("SELECT COUNT(*) FROM answer")).isZero();
        }
    }

    @Test
    void 참여_완료_처리와_수정이_동시에_와도_완료_상태가_유지된다() throws Exception {
        for (int round = 0; round < 40; round++) {
            long memberId = 100 + round;
            Long submissionId = submit(memberId);

            List<Object> results = runTogether(List.of(
                    () -> submissionService.update(postId, memberId, answer("수정")),
                    () -> formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED)));

            // 운영진 처리는 항상 성공, 수정은 먼저 끝나 성공하거나 완료 뒤라서 409
            assertThat(results.get(1)).isNotInstanceOf(Throwable.class);
            Object update = results.get(0);
            assertThat(update instanceof Throwable ? isCompleted(update) : true)
                    .as("수정 결과: %s", update).isTrue();

            String status = jdbc.queryForObject(
                    "SELECT status FROM submission WHERE id = ?", String.class, submissionId);
            assertThat(status).as("round %d: 수정이 참여 완료를 덮어썼다", round).isEqualTo("COMPLETED");
        }
    }
}
