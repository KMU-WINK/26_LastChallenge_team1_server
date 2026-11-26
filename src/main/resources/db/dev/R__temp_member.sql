-- =========================================================
-- 로컬 개발 전용: 임시 회원 테이블과 테스트 회원
--   제출자 관리(FORM-03·05·06)가 읽는 id, name, student_no만 있다.
--   홈페이지 회원 테이블이 확정되면 이 파일은 지운다.
--
-- 운영에는 적용하지 않는다 (local 프로필에서만 db/dev를 읽는다. application.yml 참고).
-- 반복 마이그레이션(R__)이라 파일 내용이 바뀌면 다시 실행된다. 여러 번 돌려도 안전하게 쓴다.
-- =========================================================

CREATE TABLE IF NOT EXISTS member (
                        id          BIGINT       NOT NULL AUTO_INCREMENT,
                        name        VARCHAR(50)  NOT NULL,
                        student_no  VARCHAR(20)  NOT NULL,
                        PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 이미 있는 id는 건드리지 않는다 (팀원이 로컬에서 직접 고친 회원을 덮어쓰지 않도록)
INSERT IGNORE INTO member (id, name, student_no) VALUES
                                              (1,  '운영진',  '20210001'),
                                              (10, '김호', '20223067');
