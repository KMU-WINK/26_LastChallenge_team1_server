CREATE TABLE post (
                      id            BIGINT       NOT NULL AUTO_INCREMENT,
                      -- term          VARCHAR(10)  NOT NULL,                  -- '2026-2'
                      -- type          VARCHAR(10)  NOT NULL,                  -- STUDY / EVENT
                      title         VARCHAR(100) NOT NULL,
                      summary       VARCHAR(100) NOT NULL,                  -- 목록의 한 줄 설명
                      -- description   TEXT         NULL,                      -- 상세의 긴 설명
                      status        VARCHAR(10)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT / OPEN / CLOSED
                      opens_at      DATETIME(6)  NOT NULL,
                      closes_at     DATETIME(6)  NOT NULL,
                      published_at  DATETIME(6)  NULL,
                      created_by    BIGINT       NOT NULL,
                      created_at    DATETIME(6)  NOT NULL,
                      updated_at    DATETIME(6)  NOT NULL,
                      deleted_at    DATETIME(6)  NULL,
                      PRIMARY KEY (id),
                      KEY idx_post_status_closes_at (status, closes_at)
                      # KEY idx_post_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;