CREATE TABLE merchant_payment_config (
 id BIGSERIAL PRIMARY KEY, singleton_key VARCHAR(20) NOT NULL UNIQUE,
 merchant_name VARCHAR(120) NOT NULL, upi_id VARCHAR(180) NOT NULL,
 bank_name VARCHAR(100) NOT NULL, account_last_four VARCHAR(4), active BOOLEAN NOT NULL DEFAULT FALSE,
 updated_at TIMESTAMPTZ NOT NULL, updated_by VARCHAR(180) NOT NULL
);
CREATE TABLE upi_payment_requests (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id),
 deposit_id BIGINT NOT NULL UNIQUE REFERENCES deposits(id), reference VARCHAR(100) NOT NULL UNIQUE,
 amount NUMERIC(18,2) NOT NULL, merchant_upi_id VARCHAR(180) NOT NULL,
 merchant_name VARCHAR(120) NOT NULL, status VARCHAR(30) NOT NULL,
 provider VARCHAR(40), provider_transaction_id VARCHAR(150), bank_reference VARCHAR(150),
 verification_evidence VARCHAR(500), detected_at TIMESTAMPTZ, verified_at TIMESTAMPTZ,
 wallet_credited_at TIMESTAMPTZ, expires_at TIMESTAMPTZ NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_upi_payment_user_date ON upi_payment_requests(user_id,created_at DESC);
CREATE INDEX idx_upi_payment_status_date ON upi_payment_requests(status,created_at DESC);
CREATE UNIQUE INDEX uk_upi_provider_transaction ON upi_payment_requests(provider,provider_transaction_id)
 WHERE provider_transaction_id IS NOT NULL;
CREATE TABLE payment_webhook_deliveries (
 id BIGSERIAL PRIMARY KEY, provider VARCHAR(40) NOT NULL, delivery_id VARCHAR(150) NOT NULL,
 payment_reference VARCHAR(100), event_type VARCHAR(60), accepted BOOLEAN NOT NULL,
 detail VARCHAR(500), received_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_payment_webhook_delivery UNIQUE(provider,delivery_id)
);
