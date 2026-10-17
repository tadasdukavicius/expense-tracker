CREATE TABLE account (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    currency VARCHAR(3)   NOT NULL DEFAULT 'EUR'
);

ALTER TABLE transactions
    ADD COLUMN account_id BIGINT REFERENCES account(id) ON DELETE SET NULL,
    ADD COLUMN reference VARCHAR(255);

CREATE UNIQUE INDEX uq_transactions_account_dedup_hash
    ON transactions(account_id, dedup_hash)
    WHERE account_id IS NOT NULL AND dedup_hash IS NOT NULL;

CREATE TABLE import_batch (
    id               BIGSERIAL PRIMARY KEY,
    account_id       BIGINT      NOT NULL REFERENCES account(id),
    file_name        VARCHAR(255) NOT NULL,
    imported_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'CONFIRMED')),
    new_count        INTEGER     NOT NULL DEFAULT 0 CHECK (new_count >= 0),
    duplicate_count  INTEGER     NOT NULL DEFAULT 0 CHECK (duplicate_count >= 0)
);

CREATE TABLE import_batch_row (
    id                    BIGSERIAL PRIMARY KEY,
    import_batch_id       BIGINT         NOT NULL
                          REFERENCES import_batch(id) ON DELETE CASCADE,
    statement_row_number  INTEGER        NOT NULL CHECK (statement_row_number > 0),
    booking_date          DATE           NOT NULL,
    amount                NUMERIC(19,2)  NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    description           VARCHAR(500)   NOT NULL,
    counterparty          VARCHAR(255),
    reference             VARCHAR(255),
    dedup_hash            VARCHAR(64)    NOT NULL,
    is_duplicate          BOOLEAN        NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_import_batch_row_number
        UNIQUE (import_batch_id, statement_row_number)
);