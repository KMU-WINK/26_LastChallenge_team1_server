-- =========================================================
--   post, question, question_option : 김호 담당: POST-01~06, FORM-01
-- =========================================================

-- 신청글
CREATE TABLE post (
    id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '번호',
    title        VARCHAR(100) NOT NULL                 COMMENT '제목',
    summary      VARCHAR(100) NULL                     COMMENT '한 줄 소개 (목록용, 선택)',
    description  TEXT         NOT NULL                 COMMENT '본문 (상세용)',
    status       VARCHAR(10)  NOT NULL DEFAULT 'OPEN'  COMMENT 'OPEN: 게시됨 / CLOSED: 수동 마감',
    opens_at     DATETIME(6)  NOT NULL                 COMMENT '신청 시작 (시작일 00:00:00)',
    closes_at    DATETIME(6)  NOT NULL                 COMMENT '신청 마감 (마감일 23:59:59)',
    created_by   BIGINT       NOT NULL                 COMMENT '작성한 운영진 회원 ID',
    created_at   DATETIME(6)  NOT NULL                 COMMENT '등록일시',
    updated_at   DATETIME(6)  NOT NULL                 COMMENT '수정일시',
    deleted_at   DATETIME(6)  NULL                     COMMENT '삭제일시 (소프트 삭제)',
    PRIMARY KEY (id),
    KEY idx_post_status_closes_at (status, closes_at),
    CONSTRAINT ck_post_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_post_period CHECK (opens_at < closes_at)
    -- created_by → 회원 테이블 FK는 회원 테이블이 생기면 추가
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '신청글';

-- 신청서 질문 (신청글 1 : N 질문)
CREATE TABLE question (
    id        BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '질문 ID',
    post_id   BIGINT       NOT NULL                 COMMENT '소속 신청글',
    order_no  INT          NOT NULL                 COMMENT '화면에 보일 순서 (1부터)',
    title     VARCHAR(200) NOT NULL                 COMMENT '질문 제목',
    type      VARCHAR(20)  NOT NULL                 COMMENT 'SHORT_ANSWER: 단답형 / MULTIPLE_CHOICE: 객관식',
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_post_order (post_id, order_no),
    CONSTRAINT fk_question_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT ck_question_type CHECK (type IN ('SHORT_ANSWER', 'MULTIPLE_CHOICE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '신청서 질문';

-- 객관식 문항 (질문 1 : N 문항)
CREATE TABLE question_option (
    id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '선택지 ID',
    question_id  BIGINT       NOT NULL                 COMMENT '소속 질문',
    order_no     INT          NOT NULL                 COMMENT '선택지 순서 (1부터)',
    content      VARCHAR(100) NOT NULL                 COMMENT '선택지 내용',
    PRIMARY KEY (id),
    UNIQUE KEY uk_option_question_order (question_id, order_no),
    UNIQUE KEY uk_option_question_content (question_id, content),
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES question (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '객관식 선택지';