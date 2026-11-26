package com.wink.ichibang.support;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 테스트 스키마를 비우고 마이그레이션을 처음부터 적용한다 (이전 실행의 찌꺼기에 영향받지 않게).
 *
 * clean은 스키마의 모든 테이블을 지운다. 테스트 URL이 개발 DB를 가리키면 개발 데이터가 사라지므로,
 * 이름이 _test로 끝나는 스키마가 아니면 아무것도 지우지 않고 테스트를 실패시킨다.
 */
@TestConfiguration
class CleanFlywayConfig {

    @Bean
    FlywayMigrationStrategy cleanThenMigrate(DataSource dataSource) {
        return flyway -> {
            String schema = currentSchema(dataSource);
            if (schema == null || !schema.endsWith("_test")) {
                throw new IllegalStateException("테스트는 이름이 _test로 끝나는 스키마에서만 돌릴 수 있어요 (현재: " + schema + ")");
            }
            flyway.clean();
            flyway.migrate();
        };
    }

    private static String currentSchema(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getCatalog();
        } catch (SQLException e) {
            throw new IllegalStateException("테스트 스키마 이름을 확인하지 못했어요", e);
        }
    }
}
