USE ichibang;

CREATE TABLE post (
                      id            BIGINT       NOT NULL AUTO_INCREMENT,
                      term          VARCHAR(10)  NOT NULL,
                      type          VARCHAR(10)  NOT NULL,
                      title         VARCHAR(100) NOT NULL,
                      description   TEXT         NULL,
                      status        VARCHAR(10)  NOT NULL DEFAULT 'DRAFT',
                      opens_at      DATETIME(6)  NOT NULL,
                      closes_at     DATETIME(6)  NOT NULL,
                      published_at  DATETIME(6)  NULL,
                      created_by    BIGINT       NOT NULL,
                      created_at    DATETIME(6)  NOT NULL,
                      updated_at    DATETIME(6)  NOT NULL,
                      deleted_at    DATETIME(6)  NULL,
                      PRIMARY KEY (id),
                      KEY idx_post_term_status (term, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;