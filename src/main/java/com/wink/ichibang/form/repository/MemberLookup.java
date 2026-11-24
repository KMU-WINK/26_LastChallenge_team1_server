package com.wink.ichibang.form.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class MemberLookup {
    private final NamedParameterJdbcTemplate jdbc;

    public MemberLookup(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, MemberName> find(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();

        return jdbc.query(
                "select id, name, student_no from member where id in (:ids)",
                new MapSqlParameterSource("ids", ids),
                (rs, rowNum) -> new MemberName(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("student_no")
                )
        ).stream().collect(
                Collectors.toMap(MemberName::id, Function.identity())
        );
    }

    public record MemberName(Long id, String name, String studentNo) {}
}