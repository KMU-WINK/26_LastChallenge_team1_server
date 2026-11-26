package com.wink.ichibang.support;

import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 실제 MySQL로 도는 통합 테스트용 설정.
 *
 * 유니크 제약·외래키·행 잠금(SELECT ... FOR UPDATE)은 MySQL에서만 그대로 재현되므로 실제 MySQL을 쓴다.
 * 개발 DB(ichibang)를 건드리지 않도록 전용 스키마 ichibang_test를 자동으로 만들어 쓰고,
 * 계정·비밀번호는 로컬 설정(application-local.yml)을 따른다.
 * 이 어노테이션을 쓰는 테스트끼리는 설정이 같아서 스프링 컨텍스트(와 스키마)를 함께 쓴다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/ichibang_test"
                + "?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true" // 쿼리 수를 세는 테스트용
})
public @interface MySqlIntegrationTest {
}
