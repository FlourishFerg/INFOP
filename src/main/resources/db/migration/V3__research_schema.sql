-- =========================================================
-- Research papers and keywords
-- =========================================================
CREATE TABLE research_papers (
    id               VARCHAR(50)    PRIMARY KEY,
    user_id          VARCHAR(50)    NOT NULL,
    title            VARCHAR(255)   NOT NULL,
    abstract_text    TEXT           NOT NULL,
    file_key         VARCHAR(500)   NOT NULL,
    file_url         VARCHAR(1000)  NOT NULL,
    file_size_bytes  BIGINT         NOT NULL,
    status           VARCHAR(50)    NOT NULL,
    rejection_reason TEXT,
    created_at       TIMESTAMP      WITHOUT TIME ZONE,
    updated_at       TIMESTAMP      WITHOUT TIME ZONE,

    CONSTRAINT fk_research_papers_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE research_keywords (
    id           VARCHAR(50)  PRIMARY KEY,
    research_id  VARCHAR(50)  NOT NULL,
    keyword      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_research_keywords_paper
        FOREIGN KEY (research_id)
        REFERENCES research_papers(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_research_papers_user_id   ON research_papers(user_id);
CREATE INDEX idx_research_papers_status    ON research_papers(status);
CREATE INDEX idx_research_keywords_paper   ON research_keywords(research_id);
