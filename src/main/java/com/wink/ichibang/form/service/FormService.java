package com.wink.ichibang.form.service;

import com.wink.ichibang.form.domain.ActivityEndRecord;
import com.wink.ichibang.form.dto.FormResponses.*;
import com.wink.ichibang.form.exception.FormException;
import com.wink.ichibang.form.repository.ActivityEndRepository;
import com.wink.ichibang.form.repository.MemberLookup;
import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.domain.QuestionOption;
import com.wink.ichibang.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** FORM 전용. submission/answer 엔티티는 SUB 담당자가 정의하며, 이 코드는 테이블만 읽는다. */
@Service
@RequiredArgsConstructor
public class FormService {
    private final PostRepository posts;
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final MemberLookup members;
    private final ActivityEndRepository activityEnds;

    private record Row(Long id, Long userId, String status, LocalDateTime submittedAt, LocalDateTime completedAt) { }

    @Transactional(readOnly = true)
    public MyForm myForm(Long postId, long memberId) {
        Post post = post(postId);
        LocalDate today = LocalDate.now(Post.KST);
        if (!post.isStartedOn(today)) throw notFound("POST_NOT_FOUND", "신청글을 찾을 수 없습니다.");
        List<Row> rows = jdbc.query("""
                SELECT id, user_id, status, submitted_at, completed_at FROM submission
                WHERE post_id = ? AND user_id = ? AND status IN ('SUBMITTED', 'COMPLETED')
                """, (rs, i) -> row(rs), postId, memberId);
        Row mine = rows.isEmpty() ? null : rows.get(0);
        boolean applying = post.statusOn(today) == PostStatus.OPEN;
        return new MyForm(postId, post.getTitle(), mine == null && applying,
                mine != null && mine.status().equals("SUBMITTED") && applying,
                mine == null ? null : mine.id(), mine == null ? null : mine.status(),
                mine == null ? null : mine.submittedAt(), questions(post),
                mine == null ? List.of() : answers(mine.id(), post));
    }

    @Transactional(readOnly = true)
    public Applicants applicants(Long postId, int page, int size) {
        Post post = post(postId);
        long total = count(postId, null);
        List<Row> rows = jdbc.query("""
                SELECT id, user_id, status, submitted_at, completed_at FROM submission
                WHERE post_id = ? AND status IN ('SUBMITTED', 'COMPLETED')
                ORDER BY submitted_at DESC, id DESC LIMIT ? OFFSET ?
                """, (rs, i) -> row(rs), postId, size, (long) page * size);
        Map<Long, MemberLookup.MemberName> names = members.find(rows.stream().map(Row::userId).toList());
        Map<Long, List<AnswerView>> responses = answerMap(rows, post);
        return new Applicants(total, count(postId, "COMPLETED"), count(postId, "SUBMITTED"),
                rows.stream().map(r -> applicant(r, names, responses)).toList(), page, size,
                total, (int) ((total + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public SubmissionDetail detail(Long postId, Long submissionId) {
        Post post = post(postId);
        List<Row> rows = jdbc.query("""
                SELECT id, user_id, status, submitted_at, completed_at FROM submission
                WHERE id = ? AND post_id = ? AND status IN ('SUBMITTED', 'COMPLETED')
                """, (rs, i) -> row(rs), submissionId, postId);
        if (rows.isEmpty()) throw notFound("SUBMISSION_NOT_FOUND", "제출을 찾을 수 없습니다.");
        Row r = rows.get(0);
        return new SubmissionDetail(postId, post.getTitle(),
                applicant(r, members.find(List.of(r.userId())), answerMap(rows, post)), questions(post));
    }

    @Transactional
    public ActivityEnd endActivity(Long postId) {
        Post post = post(postId);
        LocalDateTime now = LocalDateTime.now(Post.KST);
        if (post.statusOn(now.toLocalDate()) != PostStatus.CLOSED)
            throw conflict("APPLICATION_OPEN", "모집 마감 후 활동을 종료해 주세요.");
        if (activityEnds.existsById(postId))
            throw conflict("ACTIVITY_ALREADY_ENDED", "이미 활동이 종료되었습니다.");
        activityEnds.saveAndFlush(new ActivityEndRecord(postId, now));
        return new ActivityEnd(postId, now);
    }

    @Transactional
    public Completion complete(Long postId, Long submissionId) {
        post(postId);
        if (!activityEnds.existsById(postId))
            throw conflict("ACTIVITY_NOT_ENDED", "활동 종료를 먼저 확정해 주세요.");
        LocalDateTime now = LocalDateTime.now(Post.KST);
        int updated = jdbc.update("""
                UPDATE submission SET status = 'COMPLETED', completed_at = ?, updated_at = ?
                WHERE id = ? AND post_id = ? AND status = 'SUBMITTED'
                """, now, now, submissionId, postId);
        if (updated == 0) {
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM submission WHERE id = ? AND post_id = ?",
                    Integer.class, submissionId, postId);
            if (exists == null || exists == 0)
                throw notFound("SUBMISSION_NOT_FOUND", "제출을 찾을 수 없습니다.");
            throw conflict("INVALID_SUBMISSION_STATUS", "제출됨 상태에서만 참여 완료 처리할 수 있습니다.");
        }
        return new Completion(submissionId, "COMPLETED", now);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long postId) {
        Post post = post(postId);
        List<Row> rows = jdbc.query("""
                SELECT id, user_id, status, submitted_at, completed_at FROM submission
                WHERE post_id = ? AND status IN ('SUBMITTED', 'COMPLETED')
                ORDER BY submitted_at DESC, id DESC
                """, (rs, i) -> row(rs), postId);
        Map<Long, MemberLookup.MemberName> names = members.find(rows.stream().map(Row::userId).toList());
        Map<Long, List<AnswerView>> responses = answerMap(rows, post);
        StringBuilder csv = new StringBuilder("\ufeff제출ID,이름,학번,신청여부,참여완료여부,제출시각,완료시각");
        for (Question q : post.getQuestions()) csv.append(',').append(cell(q.getContent()));
        csv.append("\r\n");
        for (Row r : rows) {
            MemberLookup.MemberName m = names.get(r.userId());
            csv.append(r.id()).append(',').append(cell(m == null ? "" : m.name()))
                    .append(',').append(cell(m == null ? "" : m.studentNo()))
                    .append(",신청,").append(r.status().equals("COMPLETED") ? "완료" : "미완료")
                    .append(',').append(cell(r.submittedAt())).append(',').append(cell(r.completedAt()));
            for (Question q : post.getQuestions()) {
                String value = responses.getOrDefault(r.id(), List.of()).stream()
                        .filter(a -> a.questionId().equals(q.getId()))
                        .map(a -> a.optionContent() != null ? a.optionContent() : a.text())
                        .filter(v -> v != null && !v.isBlank())
                        .collect(Collectors.joining("; "));
                csv.append(',').append(cell(value));
            }
            csv.append("\r\n");
        }
        return csv.toString();
    }

    private long count(Long postId, String status) {
        String clause = status == null ? "status IN ('SUBMITTED', 'COMPLETED')" : "status = ?";
        Long value = status == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM submission WHERE post_id = ? AND " + clause, Long.class, postId)
                : jdbc.queryForObject("SELECT COUNT(*) FROM submission WHERE post_id = ? AND " + clause, Long.class, postId, status);
        return value == null ? 0 : value;
    }

    private Map<Long, List<AnswerView>> answerMap(List<Row> rows, Post post) {
        if (rows.isEmpty()) return Map.of();
        Map<Long, Question> questions = post.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        return namedJdbc.query("SELECT submission_id, question_id, option_id, text FROM answer WHERE submission_id IN (:ids)",
                new MapSqlParameterSource("ids", rows.stream().map(Row::id).toList()), rs -> {
                    Map<Long, List<AnswerView>> result = new java.util.HashMap<>();
                    while (rs.next()) {
                        Question q = questions.get(rs.getLong("question_id"));
                        if (q == null) continue;
                        Long optionId = (Long) rs.getObject("option_id");
                        String option = q.getOptions().stream().filter(o -> o.getId().equals(optionId))
                                .map(QuestionOption::getContent).findFirst().orElse(null);
                        result.computeIfAbsent(rs.getLong("submission_id"), ignored -> new java.util.ArrayList<>())
                                .add(new AnswerView(q.getId(), optionId, option, rs.getString("text")));
                    }
                    return result;
                });
    }

    private List<AnswerView> answers(Long submissionId, Post post) {
        return answerMap(List.of(new Row(submissionId, 0L, "SUBMITTED", null, null)), post)
                .getOrDefault(submissionId, List.of());
    }

    private Applicant applicant(Row r, Map<Long, MemberLookup.MemberName> names,
                                Map<Long, List<AnswerView>> responses) {
        MemberLookup.MemberName m = names.get(r.userId());
        return new Applicant(r.id(), r.userId(), m == null ? null : m.name(),
                m == null ? null : m.studentNo(), r.status(), r.submittedAt(), r.completedAt(),
                responses.getOrDefault(r.id(), List.of()));
    }

    private Post post(Long id) {
        return posts.findById(id).orElseThrow(() -> notFound("POST_NOT_FOUND", "신청글을 찾을 수 없습니다."));
    }

    private List<QuestionView> questions(Post p) {
        return p.getQuestions().stream().map(q -> new QuestionView(q.getId(), q.getContent(),
                q.getDescription(), q.getType(), q.getOptions().stream()
                .map(o -> new OptionView(o.getId(), o.getContent())).toList())).toList();
    }

    private static Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(rs.getLong("id"), rs.getLong("user_id"), rs.getString("status"),
                rs.getTimestamp("submitted_at").toLocalDateTime(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime());
    }

    private static String cell(Object input) {
        String value = input == null ? "" : input.toString();
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static FormException notFound(String code, String message) {
        return new FormException(HttpStatus.NOT_FOUND, code, message);
    }

    private static FormException conflict(String code, String message) {
        return new FormException(HttpStatus.CONFLICT, code, message);
    }
}
