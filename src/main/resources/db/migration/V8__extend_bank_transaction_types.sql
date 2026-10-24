
ALTER TABLE bank_transactions
DROP CONSTRAINT chk_bank_transactions_type;

ALTER TABLE bank_transactions
ADD CONSTRAINT chk_bank_transactions_type
CHECK (
    transaction_type IN (
        'DEPOSIT',
        'WITHDRAWAL',
        'TRANSFER_OUT',
        'TRANSFER_IN'
    )
);
