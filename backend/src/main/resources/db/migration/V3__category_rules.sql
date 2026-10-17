CREATE TABLE category_rule (
    id           BIGSERIAL PRIMARY KEY,
    match_field  VARCHAR(20)  NOT NULL CHECK (match_field IN ('DESCRIPTION', 'COUNTERPARTY')),
    pattern      VARCHAR(255) NOT NULL,
    category_id  BIGINT       NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    priority     INTEGER      NOT NULL DEFAULT 100
);

CREATE INDEX idx_category_rule_priority ON category_rule(priority);

ALTER TABLE transactions
    ADD COLUMN manually_categorized BOOLEAN NOT NULL DEFAULT FALSE;