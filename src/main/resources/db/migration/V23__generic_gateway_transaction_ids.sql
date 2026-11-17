ALTER TABLE deposits RENAME COLUMN payu_payment_id TO gateway_transaction_id;
ALTER INDEX IF EXISTS idx_deposit_payu_id RENAME TO idx_deposit_gateway_transaction_id;
ALTER INDEX IF EXISTS uk_deposits_payu_payment_id RENAME TO uk_deposits_gateway_transaction_id;
