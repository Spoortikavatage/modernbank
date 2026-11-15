ALTER TABLE bank_transfers
ADD CONSTRAINT uk_transfer_sender_idempotency
UNIQUE (from_account_id, idempotency_key);