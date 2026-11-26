package com.wink.ichibang.form.service;

import com.wink.ichibang.form.dto.FormResponses.AnswerDetail;
import com.wink.ichibang.form.dto.FormResponses.Applicants;
import com.wink.ichibang.form.dto.FormResponses.MyForm;
import com.wink.ichibang.form.dto.FormResponses.StatusChangeResponse;
import com.wink.ichibang.form.dto.FormResponses.SubmissionDetail;
import com.wink.ichibang.form.service.FormService.CsvFile;
import com.wink.ichibang.global.exception.BusinessException;
import com.wink.ichibang.global.exception.ErrorCode;
import com.wink.ichibang.global.time.TimeZones;
import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.domain.QuestionDraft;
import com.wink.ichibang.post.domain.QuestionDraft.OptionDraft;
import com.wink.ichibang.post.domain.QuestionType;
import com.wink.ichibang.post.repository.PostRepository;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import com.wink.ichibang.submission.dto.request.SubmissionRequest;
import com.wink.ichibang.submission.dto.request.SubmissionRequest.AnswerRequest;
import com.wink.ichibang.submission.service.SubmissionService;
import com.wink.ichibang.support.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 신청서 조회(FORM-02)와 제출자 관리(FORM-03~06)의 결과를 실제 MySQL로 확인한다.
 * 제출은 SubmissionService로 만들어서, 제출 도메인의 저장 방식이 바뀌어도 form이 같은 결과를 내는지 본다.
 */
@MySqlIntegrationTest
class FormServiceIntegrationTest {

    private static final long ALICE = 10L;
    private static final long BOB = 11L;
    private static final long CAROL = 12L;
    private static final long NO_MEMBER_ROW = 99L; // member 테이블에 없는 회원

    @Autowired
    FormService formService;
    @Autowired
    SubmissionService submissionService;
    @Autowired
    PostRepository postRepository;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManagerFactory entityManagerFactory;

    Long postId;
    Question single;
    Question multi;
    Question shortText;
    Question longText;

    @BeforeEach
    void setUp() {
        // 자식 테이블부터 비운다 (외래키 순서)
        for (String table : List.of("answer_option", "answer", "submission", "question_option", "question", "post")) {
            jdbc.update("DELETE FROM " + table);
        }
        // member는 엔티티가 없는 임시 테이블이라 직접 만든다
        jdbc.execute("CREATE TABLE IF NOT EXISTS member (id BIGINT NOT NULL AUTO_INCREMENT, "
                + "name VARCHAR(50) NOT NULL, student_no VARCHAR(20) NOT NULL, PRIMARY KEY (id)) "
                + "ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
        jdbc.update("DELETE FROM member");
        jdbc.update("INSERT INTO member (id, name, student_no) VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)",
                ALICE, "앨리스", "20220010", BOB, "밥", "20220011", CAROL, "캐롤", "20220012");

        LocalDate today = LocalDate.now(TimeZones.KST);
        Post post = postRepository.save(Post.create(PostType.STUDY, "스프링/스터디", null, "본문",
                today.minusDays(1), today.plusDays(7), 1L, List.of(
                        draft("학년", QuestionType.SINGLE, "1학년", "2학년"),
                        draft("시간대", QuestionType.MULTI, "평일 저녁", "주말 오후"),
                        draft("GitHub", QuestionType.SHORT),
                        draft("지원 동기", QuestionType.LONG))));
        postId = post.getId();
        single = post.getQuestions().get(0);
        multi = post.getQuestions().get(1);
        shortText = post.getQuestions().get(2);
        longText = post.getQuestions().get(3);
    }

    private static QuestionDraft draft(String content, QuestionType type, String... options) {
        List<OptionDraft> drafts = java.util.Arrays.stream(options).map(o -> new OptionDraft(null, o)).toList();
        return new QuestionDraft(null, content, null, type, drafts);
    }

    private Long optionId(Question question, int index) {
        return question.getOptions().get(index).getId();
    }

    /** 객관식 두 개와 단답에 답한 제출 (서술은 비워 둔다) */
    private Long submitFull(long memberId, String github) {
        return submissionService.submit(postId, memberId, new SubmissionRequest(List.of(
                new AnswerRequest(single.getId(), List.of(optionId(single, 1)), null),
                // 일부러 뒤집어 보낸다. 결과는 질문의 선택지 순서로 나와야 한다
                new AnswerRequest(multi.getId(), List.of(optionId(multi, 1), optionId(multi, 0)), null),
                new AnswerRequest(shortText.getId(), null, github)))).submissionId();
    }

    private static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    // ===== FORM-02 신청서 조회 =====

    @Test
    void 신청하지_않았으면_mySubmission이_null이다() {
        MyForm form = formService.myForm(postId, ALICE);

        assertThat(form.title()).isEqualTo("스프링/스터디");
        assertThat(form.questions()).hasSize(4);
        assertThat(form.questions().get(0).options()).extracting("content").containsExactly("1학년", "2학년");
        assertThat(form.questions().get(2).options()).isEmpty();
        assertThat(form.mySubmission()).isNull();
    }

    @Test
    void 신청했으면_내_답변이_질문_순서로_나오고_답하지_않은_질문은_빠진다() {
        Long submissionId = submitFull(ALICE, "https://github.com/alice");

        MyForm form = formService.myForm(postId, ALICE);

        assertThat(form.mySubmission().submissionId()).isEqualTo(submissionId);
        assertThat(form.mySubmission().status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(form.mySubmission().answers()).hasSize(3);
        assertThat(form.mySubmission().answers().get(0).questionId()).isEqualTo(single.getId());
        assertThat(form.mySubmission().answers().get(0).optionIds()).containsExactly(optionId(single, 1));
        assertThat(form.mySubmission().answers().get(0).text()).isNull();
        assertThat(form.mySubmission().answers().get(1).optionIds())
                .containsExactly(optionId(multi, 0), optionId(multi, 1));
        assertThat(form.mySubmission().answers().get(2).text()).isEqualTo("https://github.com/alice");
        assertThat(form.mySubmission().answers().get(2).optionIds()).isNull();
    }

    @Test
    void 다른_부원의_제출은_내_신청서에_나오지_않는다() {
        submitFull(BOB, "bob");

        assertThat(formService.myForm(postId, ALICE).mySubmission()).isNull();
    }

    @Test
    void 없는_글은_404_마감된_글은_400이다() {
        assertCode(() -> formService.myForm(999_999L, ALICE), ErrorCode.POST_NOT_FOUND);

        LocalDate today = LocalDate.now(TimeZones.KST);
        Post ended = postRepository.save(Post.create(PostType.EVENT, "끝난 행사", null, "본문",
                today.minusDays(10), today.minusDays(1), 1L, List.of(draft("이름", QuestionType.SHORT))));
        assertCode(() -> formService.myForm(ended.getId(), ALICE), ErrorCode.APPLY_POST_NOT_OPEN);
    }

    // ===== FORM-03 제출자 목록 =====

    @Test
    void 제출자_목록은_최신순이고_이름과_학번이_붙는다() {
        submitFull(ALICE, "a");
        submitFull(BOB, "b");
        submitFull(NO_MEMBER_ROW, "x");

        Applicants applicants = formService.applicants(postId, null, 0, 20);

        assertThat(applicants.post().id()).isEqualTo(postId);
        assertThat(applicants.counts().total()).isEqualTo(3);
        assertThat(applicants.totalElements()).isEqualTo(3);
        assertThat(applicants.totalPages()).isEqualTo(1);
        // 제출 시각이 같은 초면 나중에 만든 제출(id가 큰 쪽)이 먼저
        assertThat(applicants.content()).extracting("name").containsExactly(null, "밥", "앨리스");
        assertThat(applicants.content()).extracting("studentNo").containsExactly(null, "20220011", "20220010");
        assertThat(applicants.content()).extracting("status").containsOnly(SubmissionStatus.SUBMITTED);
        assertThat(applicants.content().get(0).submittedAt()).isEqualTo(LocalDate.now(TimeZones.KST));
    }

    @Test
    void 상태_필터와_페이지는_목록에만_적용되고_숫자는_글_전체_기준이다() {
        Long first = submitFull(ALICE, "a");
        submitFull(BOB, "b");
        submitFull(CAROL, "c");
        formService.changeStatus(postId, first, SubmissionStatus.COMPLETED);

        Applicants completed = formService.applicants(postId, SubmissionStatus.COMPLETED, 0, 20);
        assertThat(completed.content()).extracting("name").containsExactly("앨리스");
        assertThat(completed.totalElements()).isEqualTo(1);
        assertThat(completed.counts().total()).isEqualTo(3);
        assertThat(completed.counts().submitted()).isEqualTo(2);
        assertThat(completed.counts().completed()).isEqualTo(1);

        Applicants submittedPage2 = formService.applicants(postId, SubmissionStatus.SUBMITTED, 1, 1);
        assertThat(submittedPage2.content()).extracting("name").containsExactly("밥");
        assertThat(submittedPage2.totalElements()).isEqualTo(2);
        assertThat(submittedPage2.totalPages()).isEqualTo(2);
        assertThat(submittedPage2.page()).isEqualTo(1);
        assertThat(submittedPage2.size()).isEqualTo(1);

        Applicants beyond = formService.applicants(postId, null, 5, 20);
        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.totalElements()).isEqualTo(3);
    }

    @Test
    void 제출이_없으면_빈_목록이고_없는_글은_404다() {
        Applicants empty = formService.applicants(postId, null, 0, 20);
        assertThat(empty.content()).isEmpty();
        assertThat(empty.counts().total()).isZero();
        assertThat(empty.totalPages()).isZero();

        assertCode(() -> formService.applicants(999_999L, null, 0, 20), ErrorCode.POST_NOT_FOUND);
    }

    // ===== FORM-06 제출 상세 =====

    @Test
    void 제출_상세는_모든_질문이_순서대로_나오고_답하지_않은_질문은_비어_있다() {
        Long submissionId = submitFull(ALICE, "https://github.com/alice");

        SubmissionDetail detail = formService.detail(postId, submissionId);

        assertThat(detail.submissionId()).isEqualTo(submissionId);
        assertThat(detail.name()).isEqualTo("앨리스");
        assertThat(detail.studentNo()).isEqualTo("20220010");
        assertThat(detail.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(detail.submittedAt().getNano()).isZero(); // 초 단위로 내려간다
        assertThat(detail.answers()).extracting(AnswerDetail::questionId)
                .containsExactly(single.getId(), multi.getId(), shortText.getId(), longText.getId());

        assertThat(detail.answers().get(0).selectedOptions()).extracting("content").containsExactly("2학년");
        assertThat(detail.answers().get(0).text()).isNull();
        assertThat(detail.answers().get(1).selectedOptions()).extracting("content")
                .containsExactly("평일 저녁", "주말 오후");
        assertThat(detail.answers().get(2).text()).isEqualTo("https://github.com/alice");
        assertThat(detail.answers().get(2).selectedOptions()).isEmpty();
        assertThat(detail.answers().get(3).text()).isNull();
        assertThat(detail.answers().get(3).selectedOptions()).isEmpty();
    }

    @Test
    void 회원_정보가_없어도_상세는_이름만_null이다() {
        Long submissionId = submitFull(NO_MEMBER_ROW, "x");

        SubmissionDetail detail = formService.detail(postId, submissionId);

        assertThat(detail.name()).isNull();
        assertThat(detail.studentNo()).isNull();
    }

    @Test
    void 없는_제출이거나_다른_글의_제출이면_404다() {
        Long submissionId = submitFull(ALICE, "a");
        LocalDate today = LocalDate.now(TimeZones.KST);
        Post other = postRepository.save(Post.create(PostType.EVENT, "다른 글", null, "본문",
                today.minusDays(1), today.plusDays(3), 1L, List.of(draft("이름", QuestionType.SHORT))));

        assertCode(() -> formService.detail(postId, 999_999L), ErrorCode.SUBMISSION_NOT_FOUND);
        assertCode(() -> formService.detail(other.getId(), submissionId), ErrorCode.SUBMISSION_NOT_FOUND);
        assertCode(() -> formService.detail(999_999L, submissionId), ErrorCode.POST_NOT_FOUND);
    }

    // ===== FORM-04 참여 완료 처리 =====

    @Test
    void 참여_완료로_바꾸면_완료_시각이_생기고_되돌리면_사라진다() {
        Long submissionId = submitFull(ALICE, "a");

        StatusChangeResponse done = formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED);
        assertThat(done.submissionId()).isEqualTo(submissionId);
        assertThat(done.status()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(done.completedAt()).isNotNull();
        assertThat(done.completedAt().getNano()).isZero();
        assertThat(formService.detail(postId, submissionId).status()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(jdbc.queryForObject("SELECT completed_at FROM submission WHERE id = ?",
                java.sql.Timestamp.class, submissionId)).isNotNull();

        StatusChangeResponse reverted = formService.changeStatus(postId, submissionId, SubmissionStatus.SUBMITTED);
        assertThat(reverted.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(reverted.completedAt()).isNull();
        assertThat(jdbc.queryForObject("SELECT completed_at FROM submission WHERE id = ?",
                java.sql.Timestamp.class, submissionId)).isNull();
    }

    @Test
    void 이미_같은_상태로_바꾸면_409다() {
        Long submissionId = submitFull(ALICE, "a");

        assertCode(() -> formService.changeStatus(postId, submissionId, SubmissionStatus.SUBMITTED),
                ErrorCode.INVALID_STATUS_TRANSITION);

        formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED);
        assertCode(() -> formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED),
                ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void 상태_변경은_답변과_수정_일시를_건드리지_않는다() {
        Long submissionId = submitFull(ALICE, "a");
        Object updatedBefore = jdbc.queryForObject(
                "SELECT updated_at FROM submission WHERE id = ?", Object.class, submissionId);

        formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED);

        assertThat(jdbc.queryForObject("SELECT updated_at FROM submission WHERE id = ?", Object.class, submissionId))
                .isEqualTo(updatedBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM answer WHERE submission_id = ?", Long.class, submissionId))
                .isEqualTo(3L);
    }

    @Test
    void 없는_제출이나_글은_404다() {
        Long submissionId = submitFull(ALICE, "a");

        assertCode(() -> formService.changeStatus(postId, 999_999L, SubmissionStatus.COMPLETED),
                ErrorCode.SUBMISSION_NOT_FOUND);
        assertCode(() -> formService.changeStatus(999_999L, submissionId, SubmissionStatus.COMPLETED),
                ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 같은_제출을_동시에_완료_처리하면_한_번만_성공하고_나머지는_409다() throws Exception {
        for (int round = 0; round < 15; round++) {
            Long submissionId = submitFull(1000 + round, "r" + round);

            List<Object> results = runTogether(List.of(
                    () -> formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED),
                    () -> formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED),
                    () -> formService.changeStatus(postId, submissionId, SubmissionStatus.COMPLETED)));

            long succeeded = results.stream().filter(r -> r instanceof StatusChangeResponse).count();
            long conflicts = results.stream().filter(r -> r instanceof BusinessException e
                    && e.getErrorCode() == ErrorCode.INVALID_STATUS_TRANSITION).count();
            assertThat(succeeded).as("round %d: %s", round, results).isEqualTo(1);
            assertThat(conflicts).as("round %d: %s", round, results).isEqualTo(2);
        }
    }

    // ===== FORM-05 CSV 내보내기 =====

    @Test
    void CSV는_질문_열과_제출_행을_최신순으로_내려준다() {
        Long first = submitFull(ALICE, "=1+1");   // 엑셀 수식으로 읽히지 않게 막아야 한다
        submissionService.submit(postId, BOB, new SubmissionRequest(List.of(
                new AnswerRequest(single.getId(), List.of(optionId(single, 0)), null),
                new AnswerRequest(longText.getId(), null, "줄바꿈\n\"따옴표\", 쉼표"))));
        formService.changeStatus(postId, first, SubmissionStatus.COMPLETED);

        CsvFile csv = formService.exportCsv(postId);

        assertThat(csv.fileName()).startsWith("신청_스프링_스터디_").endsWith(".csv"); // "/"는 "_"로
        String[] lines = csv.content().split("\r\n");
        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("﻿\"이름\",\"학번\",\"제출일\",\"상태\","
                + "\"학년\",\"시간대\",\"GitHub\",\"지원 동기\"");
        String today = LocalDate.now(TimeZones.KST).toString();
        // 최신 제출(밥)이 먼저. 줄바꿈이 들어간 칸은 따옴표 안에 그대로 있어 줄이 더 나뉜다
        assertThat(csv.content()).contains("\"밥\",\"20220011\",\"" + today + "\",\"제출됨\",\"1학년\",\"\",\"\","
                + "\"줄바꿈\n\"\"따옴표\"\", 쉼표\"");
        assertThat(csv.content()).contains("\"앨리스\",\"20220010\",\"" + today + "\",\"참여 완료\","
                + "\"2학년\",\"평일 저녁;주말 오후\",\"'=1+1\",\"\"");
        assertThat(csv.content().indexOf("\"밥\"")).isLessThan(csv.content().indexOf("\"앨리스\""));
    }

    @Test
    void 제출이_없으면_CSV는_헤더만_있다() {
        CsvFile csv = formService.exportCsv(postId);

        assertThat(csv.content().split("\r\n")).hasSize(1);
        assertCode(() -> formService.exportCsv(999_999L), ErrorCode.POST_NOT_FOUND);
    }

    // ===== 쿼리 수 (제출이 늘어도 쿼리가 따라 늘지 않아야 한다) =====

    private long statementsFor(Runnable action) {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    @Test
    void CSV와_목록의_쿼리_수는_제출_수와_상관없다() {
        for (int i = 0; i < 3; i++) {
            submitFull(2000 + i, "g" + i);
        }
        long csvWithFew = statementsFor(() -> formService.exportCsv(postId));
        long listWithFew = statementsFor(() -> formService.applicants(postId, null, 0, 50));

        for (int i = 3; i < 40; i++) {
            submitFull(2000 + i, "g" + i);
        }
        long csvWithMany = statementsFor(() -> formService.exportCsv(postId));
        long listWithMany = statementsFor(() -> formService.applicants(postId, null, 0, 50));

        // 선택지는 답변 100개 단위로 묶어 읽으므로(@BatchSize) 한두 개는 늘 수 있지만, 제출마다 늘어나면 안 된다
        assertThat(csvWithMany).as("CSV 쿼리 수").isLessThanOrEqualTo(csvWithFew + 2);
        assertThat(listWithMany).as("목록 쿼리 수").isEqualTo(listWithFew);
    }

    /** 모든 작업을 같은 순간에 시작시키고 결과(값 또는 예외)를 순서대로 돌려준다 */
    private static List<Object> runTogether(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Object>> futures = new java.util.ArrayList<>();
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
            List<Object> results = new java.util.ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
