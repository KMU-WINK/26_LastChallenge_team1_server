package com.wink.ichibang.submission.history;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MySubmissionHistoryService {
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public MySubmissionHistoryResponse.Page getMine(long memberId, int page, int size) {
        LocalDate today = LocalDate.now(Post.KST);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", size)
                .addValue("offset", (long) page * size);

        String fromAndWhere = """
                FROM submission s
                JOIN post p ON p.id = s.post_id
                WHERE s.member_id = :memberId
                  AND s.status IN ('SUBMITTED', 'COMPLETED')
                  AND p.deleted_at IS NULL
                """;

        Long result = jdbc.queryForObject(
                "SELECT COUNT(*) " + fromAndWhere, params, Long.class);
        long total = result == null ? 0 : result;

        record Row(
                long postId, PostType type, String title, String summary,
                PostStatus postStatus, Integer dDay,
                LocalDate submittedAt, String myStatus
        ) {}

        List<Row> rows = jdbc.query(
                """
                SELECT p.id AS post_id, p.type, p.title, p.summary,
                       p.status AS post_status, p.start_date, p.end_date,
                       s.submitted_at, s.status AS submission_status
                """
                        + fromAndWhere
                        + " ORDER BY s.submitted_at DESC, s.id DESC LIMIT :size OFFSET :offset",
                params,
                (rs, rowNum) -> {
                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    boolean closed = "CLOSED".equals(rs.getString("post_status"))
                            || today.isAfter(end);

                    Integer dDay = closed || today.isBefore(start)
                            ? null
                            : (int) ChronoUnit.DAYS.between(today, end);

                    return new Row(
                            rs.getLong("post_id"),
                            PostType.valueOf(rs.getString("type")),
                            rs.getString("title"),
                            rs.getString("summary"),
                            closed ? PostStatus.CLOSED : PostStatus.OPEN,
                            dDay,
                            rs.getTimestamp("submitted_at")
                                    .toLocalDateTime().toLocalDate(),
                            rs.getString("submission_status")
                    );
                }
        );

        List<MySubmissionHistoryResponse.Item> content = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            content.add(new MySubmissionHistoryResponse.Item(
                    total - (long) page * size - i,
                    row.postId(),
                    row.type(),
                    row.title(),
                    row.summary(),
                    row.postStatus(),
                    row.dDay(),
                    row.submittedAt(),
                    row.myStatus()
            ));
        }

        return new MySubmissionHistoryResponse.Page(
                content, page, size, total,
                (int) ((total + size - 1) / size)
        );
    }
}