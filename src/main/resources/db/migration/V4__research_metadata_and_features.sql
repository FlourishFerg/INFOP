-- =========================================================
-- Extended research metadata fields
-- =========================================================
ALTER TABLE research_papers
    ADD COLUMN authors           TEXT         NOT NULL DEFAULT 'Unknown',
    ADD COLUMN institution       VARCHAR(255) NOT NULL DEFAULT 'Unspecified',
    ADD COLUMN publication_year  INT          NOT NULL DEFAULT 2000,
    ADD COLUMN research_field    VARCHAR(255) NOT NULL DEFAULT 'Unspecified',
    ADD COLUMN country_of_study  VARCHAR(255) NOT NULL DEFAULT 'Unspecified',
    ADD COLUMN methodology       TEXT         NOT NULL DEFAULT 'Unspecified',
    ADD COLUMN file_type         VARCHAR(20)  NOT NULL DEFAULT 'PDF';

CREATE INDEX idx_research_papers_field       ON research_papers(research_field);
CREATE INDEX idx_research_papers_institution ON research_papers(institution);
CREATE INDEX idx_research_papers_country     ON research_papers(country_of_study);
CREATE INDEX idx_research_papers_year        ON research_papers(publication_year);

-- =========================================================
-- Dashboard notifications
-- =========================================================
CREATE TABLE notifications (
    id          VARCHAR(50)  PRIMARY KEY,
    user_id     VARCHAR(50)  NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT         NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    WITHOUT TIME ZONE,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);

-- =========================================================
-- Research share tokens
-- =========================================================
CREATE TABLE research_share_tokens (
    id           VARCHAR(50)  PRIMARY KEY,
    research_id  VARCHAR(50)  NOT NULL,
    token        VARCHAR(100) NOT NULL UNIQUE,
    expires_at   TIMESTAMP    WITHOUT TIME ZONE NOT NULL,
    created_at   TIMESTAMP    WITHOUT TIME ZONE,

    CONSTRAINT fk_research_share_tokens_paper
        FOREIGN KEY (research_id)
        REFERENCES research_papers(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_research_share_tokens_token ON research_share_tokens(token);
