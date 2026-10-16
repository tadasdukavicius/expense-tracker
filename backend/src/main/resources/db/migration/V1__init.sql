CREATE TABLE category (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(7)
);

CREATE TABLE transactions (
    id            BIGSERIAL PRIMARY KEY,
    booking_date  DATE           NOT NULL,
    amount        NUMERIC(19,2)  NOT NULL,
    currency      VARCHAR(3)        NOT NULL DEFAULT 'EUR',
    description   VARCHAR(500)   NOT NULL,
    counterparty  VARCHAR(255),
    category_id   BIGINT REFERENCES category(id) ON DELETE SET NULL,
    dedup_hash    VARCHAR(64)
);

CREATE INDEX idx_transactions_booking_date ON transactions(booking_date);
CREATE INDEX idx_transactions_category ON transactions(category_id);