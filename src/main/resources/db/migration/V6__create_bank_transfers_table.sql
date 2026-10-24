CREATE TABLE bank_transfers (
    id BIGSERIAL PRIMARY KEY,

    reference_id VARCHAR(36) NOT NULL UNIQUE,

    from_account_id BIGINT NOT NULL
        REFERENCES bank_accounts(id),

    to_account_id BIGINT NOT NULL
        REFERENCES bank_accounts(id),

    amount NUMERIC(19, 2) NOT NULL
        CHECK (amount > 0),

    description VARCHAR(255),

    idempotency_key VARCHAR(100) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_different_transfer_accounts
        CHECK (from_account_id <> to_account_id),

    CONSTRAINT uq_transfer_sender_idempotency
        UNIQUE (from_account_id, idempotency_key)
);

CREATE INDEX idx_bank_transfers_from_account
    ON bank_transfers(from_account_id);

CREATE INDEX idx_bank_transfers_to_account
    ON bank_transfers(to_account_id);

ALTER TABLE bank_transactions
    ADD COLUMN transfer_id BIGINT
        REFERENCES bank_transfers(id);

CREATE INDEX idx_bank_transactions_transfer
    ON bank_transactions(transfer_id);