package com.wink.ichibang.form.service;

import com.wink.ichibang.form.dto.FormResponses.*;
import com.wink.ichibang.form.exception.FormErrorCode;
import com.wink.ichibang.form.exception.FormException;
import com.wink.ichibang.form.repository.MemberLookup;
import com.wink.ichibang.form.repository.MemberLookup.MemberName;
import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.domain.QuestionOption;
import com.wink.ichibang.post.repository.PostRepository;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 신청서 조회(FORM-02)와 제출자 관리(FORM-03~06).
 * submission·answer·answer_option 테이블은 제출 도메인(SUB) 것이라 JPA 엔티티를 쓰지 않고 JDBC로 읽는다.
 * 컬럼 이름은 V1__init.sql을 따른다 (submission.member_id, 객관식 선택은 answer_option).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormService {

    private static final String SUBMISSION_COLUMNS = "id, member_id, status, submitted_at, completed_at";

    private final PostRepository posts;
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final MemberLookup members;

    // ===== FORM-02 신청서 조회 (부원) =====

    /** 시작 전·삭제 글은 404, 모집 중이 아니면 400 APPLY_POST_NOT_OPEN (SUB-02 기준) */
    public MyForm myForm(Long postId, long memberId) {
        LocalDate today = today();
        Post post = findPost(postId);
        if (!post.isStartedOn(today)) {
            throw new FormException(FormErrorCode.POST_NOT_FOUND);
        }
        if (post.statusOn(today) != PostStatus.OPEN) {
            throw new FormException(FormErrorCode.APPLY_POST_NOT_OPEN);
        }

        List<Row> mine = jdbc.query(
                "SELECT " + SUBMISSION_COLUMNS + " FROM submission WHERE post_id = ? AND member_id = ?",
                FormService::row, postId, memberId);

        MySubmission mySubmission = null;
        if (!mine.isEmpty()) {
            Row r = mine.get(0);
            Map<Long, StoredAnswer> answers = loadAnswers(List.of(r.id())).getOrDefault(r.id(), Map.of());
            List<MyAnswer> myAnswers = post.getQuestions().stream()
                    .filter(q -> answers.containsKey(q.getId()))
                    .map(q -> toMyAnswer(q, answers.get(q.getId())))
                    .toList();
            mySubmission = new MySubmission(r.id(), r.status(), myAnswers);
        }

        return new MyForm(post.getId(), post.getTitle(), post.getType(),
                post.getStartDate(), post.getEndDate(), questionViews(post), mySubmission);
    }

    // ===== FORM-03 제출자 목록 (운영진) =====

    /** @param status null이면 전체. counts는 status와 상관없이 글 전체 기준 */
    public Applicants applicants(Long postId, SubmissionStatus status, int page, int size) {
        Post post = findPost(postId);
        Counts counts = counts(postId);
        long totalElements = status == null ? counts.total()
                : status == SubmissionStatus.SUBMITTED ? counts.submitted() : counts.completed();

        String where = status == null ? "post_id = ?" : "post_id = ? AND status = ?";
        Object[] args = status == null
                ? new Object[]{postId, size, (long) page * size}
                : new Object[]{postId, status.name(), size, (long) page * size};
        List<Row> rows = jdbc.query(
                "SELECT " + SUBMISSION_COLUMNS + " FROM submission WHERE " + where
                        + " ORDER BY submitted_at DESC, id DESC LIMIT ? OFFSET ?",
                FormService::row, args);

        Map<Long, MemberName> names = members.find(rows.stream().map(Row::memberId).distinct().toList());
        List<ApplicantRow> content = rows.stream()
                .map(r -> {
                    MemberName m = names.get(r.memberId());
                    return new ApplicantRow(r.id(), m == null ? null : m.name(), m == null ? null : m.studentNo(),
                            r.submittedAt().toLocalDate(), r.status());
                })
                .toList();

        int totalPages = (int) ((totalElements + size - 1) / size);
        return new Applicants(postSummary(post), counts, content, page, size, totalElements, totalPages);
    }

    // ===== FORM-06 제출 상세 (운영진) =====

    public SubmissionDetail detail(Long postId, Long submissionId) {
        Post post = findPost(postId);
        Row r = findSubmission(postId, submissionId);
        MemberName m = members.find(List.of(r.memberId())).get(r.memberId());
        Map<Long, StoredAnswer> answers = loadAnswers(List.of(r.id())).getOrDefault(r.id(), Map.of());

        List<AnswerDetail> details = post.getQuestions().stream()
                .map(q -> toAnswerDetail(q, answers.get(q.getId())))
                .toList();
        return new SubmissionDetail(r.id(), m == null ? null : m.name(), m == null ? null : m.studentNo(),
                r.status(), r.submittedAt().truncatedTo(ChronoUnit.SECONDS), details);
    }

    // ===== FORM-04 참여 완료 처리·되돌리기 (운영진) =====

    /** SUBMITTED ↔ COMPLETED. 이미 같은 상태면 409 INVALID_STATUS_TRANSITION. 시점 제한 없음 */
    @Transactional
    public StatusChangeResponse changeStatus(Long postId, Long submissionId, SubmissionStatus target) {
        findPost(postId);
        Row r = findSubmission(postId, submissionId);
        if (r.status() == target) {
            throw new FormException(FormErrorCode.INVALID_STATUS_TRANSITION);
        }

        LocalDateTime completedAt = target == SubmissionStatus.COMPLETED
                ? LocalDateTime.now(Post.KST).truncatedTo(ChronoUnit.SECONDS)
                : null;
        // 동시에 두 번 누른 경우를 막으려고 현재 상태까지 조건에 넣는다
        int updated = jdbc.update(
                "UPDATE submission SET status = ?, completed_at = ? WHERE id = ? AND post_id = ? AND status = ?",
                target.name(), completedAt,
                submissionId, postId, r.status().name());
        if (updated == 0) {
            throw new FormException(FormErrorCode.INVALID_STATUS_TRANSITION);
        }
        return new StatusChangeResponse(submissionId, target, completedAt);
    }

    // ===== FORM-05 CSV 내보내기 (운영진) =====

    public CsvFile exportCsv(Long postId) {
        Post post = findPost(postId);
        List<Row> rows = jdbc.query(
                "SELECT " + SUBMISSION_COLUMNS + " FROM submission WHERE post_id = ? ORDER BY submitted_at DESC, id DESC",
                FormService::row, postId);
        Map<Long, MemberName> names = members.find(rows.stream().map(Row::memberId).distinct().toList());
        Map<Long, Map<Long, StoredAnswer>> answers = loadAnswers(rows.stream().map(Row::id).toList());

        StringBuilder csv = new StringBuilder("﻿"); // BOM: 엑셀 한글 깨짐 방지
        List<String> header = new ArrayList<>(List.of("이름", "학번", "제출일", "상태"));
        post.getQuestions().forEach(q -> header.add(q.getContent()));
        appendLine(csv, header);

        for (Row r : rows) {
            MemberName m = names.get(r.memberId());
            Map<Long, StoredAnswer> mine = answers.getOrDefault(r.id(), Map.of());
            List<String> line = new ArrayList<>(List.of(
                    m == null ? "" : nullToEmpty(m.name()),
                    m == null ? "" : nullToEmpty(m.studentNo()),
                    r.submittedAt().toLocalDate().toString(),
                    r.status() == SubmissionStatus.COMPLETED ? "참여 완료" : "제출됨"));
            for (Question q : post.getQuestions()) {
                line.add(csvAnswer(q, mine.get(q.getId())));
            }
            appendLine(csv, line);
        }

        String fileName = "신청_" + safeFileName(post.getTitle()) + "_"
                + today().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return new CsvFile(fileName, csv.toString());
    }

    public record CsvFile(String fileName, String content) {
    }

    // ===== 내부 =====

    private record Row(Long id, Long memberId, SubmissionStatus status,
                       LocalDateTime submittedAt, LocalDateTime completedAt) {
    }

    /** 질문 하나에 대한 저장된 답 */
    private record StoredAnswer(String text, List<Long> optionIds) {
    }

    private static Row row(ResultSet rs, int rowNum) throws SQLException {
        Timestamp completed = rs.getTimestamp("completed_at");
        return new Row(rs.getLong("id"), rs.getLong("member_id"),
                SubmissionStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("submitted_at").toLocalDateTime(),
                completed == null ? null : completed.toLocalDateTime());
    }

    /** 제출 ID → (질문 ID → 답) */
    private Map<Long, Map<Long, StoredAnswer>> loadAnswers(Collection<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<Long, String>> texts = new HashMap<>();
        Map<Long, Map<Long, List<Long>>> options = new HashMap<>();
        namedJdbc.query("""
                        SELECT a.submission_id, a.question_id, a.text, ao.option_id
                        FROM answer a
                        LEFT JOIN answer_option ao ON ao.answer_id = a.id
                        WHERE a.submission_id IN (:ids)
                        """,
                new MapSqlParameterSource("ids", submissionIds),
                rs -> {
                    long submissionId = rs.getLong("submission_id");
                    long questionId = rs.getLong("question_id");
                    texts.computeIfAbsent(submissionId, k -> new HashMap<>()).put(questionId, rs.getString("text"));
                    List<Long> picked = options.computeIfAbsent(submissionId, k -> new HashMap<>())
                            .computeIfAbsent(questionId, k -> new ArrayList<>());
                    long optionId = rs.getLong("option_id");
                    if (!rs.wasNull()) {
                        picked.add(optionId);
                    }
                });

        Map<Long, Map<Long, StoredAnswer>> result = new HashMap<>();
        texts.forEach((submissionId, byQuestion) -> byQuestion.forEach((questionId, text) ->
                result.computeIfAbsent(submissionId, k -> new HashMap<>())
                        .put(questionId, new StoredAnswer(text, options.get(submissionId).get(questionId)))));
        return result;
    }

    /** 고른 선택지를 질문의 표시 순서대로 */
    private static List<OptionView> selectedOptions(Question q, StoredAnswer answer) {
        if (answer == null || answer.optionIds().isEmpty()) {
            return List.of();
        }
        return q.getOptions().stream()
                .filter(o -> answer.optionIds().contains(o.getId()))
                .map(o -> new OptionView(o.getId(), o.getContent()))
                .toList();
    }

    private static MyAnswer toMyAnswer(Question q, StoredAnswer answer) {
        if (q.getType().isChoice()) {
            return new MyAnswer(q.getId(), selectedOptions(q, answer).stream().map(OptionView::id).toList(), null);
        }
        return new MyAnswer(q.getId(), null, answer.text());
    }

    private static AnswerDetail toAnswerDetail(Question q, StoredAnswer answer) {
        String text = q.getType().isChoice() || answer == null ? null : answer.text();
        return new AnswerDetail(q.getId(), q.getContent(), q.getType(), selectedOptions(q, answer), text);
    }

    /** 복수 선택은 ";"로 이어 붙인다. 답하지 않은 질문은 빈칸 */
    private static String csvAnswer(Question q, StoredAnswer answer) {
        if (answer == null) {
            return "";
        }
        if (q.getType().isChoice()) {
            return selectedOptions(q, answer).stream().map(OptionView::content).collect(Collectors.joining(";"));
        }
        return nullToEmpty(answer.text());
    }

    private Counts counts(Long postId) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbc.query("SELECT status, COUNT(*) AS cnt FROM submission WHERE post_id = ? GROUP BY status",
                rs -> {
                    byStatus.put(rs.getString("status"), rs.getLong("cnt"));
                }, postId);
        long submitted = byStatus.getOrDefault(SubmissionStatus.SUBMITTED.name(), 0L);
        long completed = byStatus.getOrDefault(SubmissionStatus.COMPLETED.name(), 0L);
        return new Counts(submitted + completed, submitted, completed);
    }

    private Row findSubmission(Long postId, Long submissionId) {
        return jdbc.query(
                        "SELECT " + SUBMISSION_COLUMNS + " FROM submission WHERE id = ? AND post_id = ?",
                        FormService::row, submissionId, postId)
                .stream().findFirst()
                .orElseThrow(() -> new FormException(FormErrorCode.SUBMISSION_NOT_FOUND));
    }

    private Post findPost(Long postId) {
        return posts.findById(postId).orElseThrow(() -> new FormException(FormErrorCode.POST_NOT_FOUND));
    }

    private PostSummary postSummary(Post post) {
        return new PostSummary(post.getId(), post.getTitle(), post.getType(), post.statusOn(today()),
                post.getStartDate(), post.getEndDate());
    }

    private static List<QuestionView> questionViews(Post post) {
        return post.getQuestions().stream()
                .map(q -> new QuestionView(q.getId(), q.getContent(), q.getDescription(), q.getType(),
                        q.getOptions().stream().map(FormService::optionView).toList()))
                .toList();
    }

    private static OptionView optionView(QuestionOption o) {
        return new OptionView(o.getId(), o.getContent());
    }

    private static void appendLine(StringBuilder csv, List<String> cells) {
        csv.append(cells.stream().map(FormService::cell).collect(Collectors.joining(","))).append("\r\n");
    }

    /** 모든 칸을 따옴표로 감싼다 (줄바꿈·쉼표 보존). =, +, -, @로 시작하면 엑셀 수식 실행을 막는다 */
    private static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String safeFileName(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static LocalDate today() {
        return LocalDate.now(Post.KST);
    }
}
