-- =========================================================
-- WINK 신청 기능 스키마 v4 - MySQL 8.0.16+
--   post, question, question_option : 신청글·신청서 질문 (POST-01~07, FORM-01)
--   submission, answer, answer_option: 제출·답변 (SUB-01~05, FORM-02~06)
--   member(회원)는 기존 테이블을 쓴다. 회원 FK는 테이블 이름이 확정되면 추가한다.
--   (로컬 개발용 임시 member 테이블과 테스트 회원은 db/dev/R__temp_member.sql)
--
-- Flyway가 실행한다. 이미 적용된 파일은 고치지 말고 V2__... 로 새 파일을 추가한다.
-- 데이터베이스는 연결 설정(spring.datasource.url)이 정하므로 USE를 쓰지 않는다.
-- =========================================================

-- 신청글
CREATE TABLE post (
                      id          BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '신청글 ID',
                      type        VARCHAR(10)   NOT NULL                 COMMENT 'STUDY: 스터디 / EVENT: 행사',
                      title       VARCHAR(200)  NOT NULL                 COMMENT '제목',
                      summary     VARCHAR(200)  NULL                     COMMENT '한 줄 소개 (목록용)',
                      content     TEXT          NOT NULL                 COMMENT '본문 (일반 텍스트)',
                      status      VARCHAR(10)   NOT NULL DEFAULT 'OPEN'  COMMENT 'OPEN: 게시 / CLOSED: 수동 마감',
                      start_date  DATE          NOT NULL                 COMMENT '신청 시작일 (0시부터)',
                      end_date    DATE          NOT NULL                 COMMENT '신청 종료일 (23:59:59까지)',
                      created_by  BIGINT        NOT NULL                 COMMENT '작성한 운영진 회원 ID',
                      created_at  DATETIME(6)   NOT NULL                 COMMENT '등록일시',
                      updated_at  DATETIME(6)   NOT NULL                 COMMENT '수정일시',
                      deleted_at  DATETIME(6)   NULL                     COMMENT '삭제일시 (소프트 삭제)',
                      PRIMARY KEY (id),
                      KEY idx_post_start_date (start_date),
                      KEY idx_post_status_end_date (status, end_date),
                      CONSTRAINT ck_post_type   CHECK (type IN ('STUDY', 'EVENT')),
                      CONSTRAINT ck_post_status CHECK (status IN ('OPEN', 'CLOSED')),
                      CONSTRAINT ck_post_period CHECK (start_date <= end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '신청글';

-- 신청서 질문 (신청글 1 : N 질문)
CREATE TABLE question (
                          id           BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '질문 ID',
                          post_id      BIGINT        NOT NULL                 COMMENT '소속 신청글',
                          order_no     INT           NOT NULL                 COMMENT '표시 순서 (1부터)',
                          content      VARCHAR(200)  NOT NULL                 COMMENT '질문',
                          description  VARCHAR(500)  NULL                     COMMENT '질문 아래 안내 (선택)',
                          type         VARCHAR(10)   NOT NULL                 COMMENT 'SINGLE / MULTI / SHORT / LONG',
                          PRIMARY KEY (id),
                          KEY idx_question_post_order (post_id, order_no),
                          CONSTRAINT fk_question_post FOREIGN KEY (post_id) REFERENCES post (id),
                          CONSTRAINT ck_question_type CHECK (type IN ('SINGLE', 'MULTI', 'SHORT', 'LONG'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '신청서 질문';

-- 객관식 선택지 (질문 1 : N 선택지). 명세의 option 테이블 (option은 MySQL 예약어라 이름 변경)
CREATE TABLE question_option (
                                 id           BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '선택지 ID',
                                 question_id  BIGINT        NOT NULL                 COMMENT '소속 질문',
                                 order_no     INT           NOT NULL                 COMMENT '표시 순서 (1부터)',
                                 content      VARCHAR(200)  NOT NULL                 COMMENT '선택지 내용',
                                 PRIMARY KEY (id),
                                 KEY idx_option_question_order (question_id, order_no),
                                 CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES question (id)
                                     ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '객관식 선택지';

-- 제출 (부원 1명이 신청글 1건에 낸 신청서 1개). 취소하면 행을 삭제한다 (SUB-03)
CREATE TABLE submission (
                            id            BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '제출 ID',
                            post_id       BIGINT       NOT NULL                     COMMENT '신청글',
                            member_id     BIGINT       NOT NULL                     COMMENT '제출한 부원 회원 ID',
                            status        VARCHAR(10)  NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED: 제출됨 / COMPLETED: 참여 완료',
                            submitted_at  DATETIME(6)  NOT NULL                     COMMENT '제출일시',
                            updated_at    DATETIME(6)  NOT NULL                     COMMENT '답변 수정일시',
                            completed_at  DATETIME(6)  NULL                         COMMENT '참여 완료 처리일시',
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_submission_post_member (post_id, member_id),
                            KEY idx_submission_member (member_id),
                            KEY idx_submission_post_status (post_id, status),
                            CONSTRAINT fk_submission_post FOREIGN KEY (post_id) REFERENCES post (id),
                            CONSTRAINT ck_submission_status CHECK (status IN ('SUBMITTED', 'COMPLETED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '신청서 제출';

-- 답변 (제출 1 : N 답변). 질문 하나에 답 하나. 답하지 않은 질문은 행이 없다
CREATE TABLE answer (
                        id             BIGINT  NOT NULL AUTO_INCREMENT  COMMENT '답변 ID',
                        submission_id  BIGINT  NOT NULL                 COMMENT '소속 제출',
                        question_id    BIGINT  NOT NULL                 COMMENT '답한 질문',
                        text           TEXT    NULL                     COMMENT '단답·서술 답변 (객관식이면 NULL)',
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_answer_submission_question (submission_id, question_id),
                        CONSTRAINT fk_answer_submission FOREIGN KEY (submission_id) REFERENCES submission (id)
                            ON DELETE CASCADE,
                        CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '신청서 답변';

-- 객관식 답변에서 고른 선택지 (답변 1 : N 선택지). SINGLE은 1개, MULTI는 여러 개
CREATE TABLE answer_option (
                               answer_id  BIGINT  NOT NULL  COMMENT '답변',
                               option_id  BIGINT  NOT NULL  COMMENT '고른 선택지',
                               PRIMARY KEY (answer_id, option_id),
                               CONSTRAINT fk_answer_option_answer FOREIGN KEY (answer_id) REFERENCES answer (id)
                                   ON DELETE CASCADE,
                               CONSTRAINT fk_answer_option_option FOREIGN KEY (option_id) REFERENCES question_option (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '객관식 답변 선택지';
