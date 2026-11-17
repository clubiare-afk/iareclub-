ALTER TABLE deposits ADD COLUMN IF NOT EXISTS payment_purpose VARCHAR(30) NOT NULL DEFAULT 'WALLET_DEPOSIT';
ALTER TABLE deposits ADD COLUMN IF NOT EXISTS product_id BIGINT;
ALTER TABLE deposits ADD COLUMN IF NOT EXISTS purchase_request_reference VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_deposit_product_payment ON deposits(product_id, status);
