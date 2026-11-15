ALTER TABLE upi_payment_requests ADD COLUMN IF NOT EXISTS youpi_payment_id VARCHAR(100);
ALTER TABLE upi_payment_requests ADD COLUMN IF NOT EXISTS checkout_url VARCHAR(1000);
ALTER TABLE upi_payment_requests ADD COLUMN IF NOT EXISTS gateway_status VARCHAR(40);
CREATE UNIQUE INDEX IF NOT EXISTS uk_upi_youpi_payment_id ON upi_payment_requests(youpi_payment_id) WHERE youpi_payment_id IS NOT NULL;

CREATE TABLE merchant_payment_config_history (
 id BIGSERIAL PRIMARY KEY, config_id BIGINT NOT NULL REFERENCES merchant_payment_config(id),
 merchant_name VARCHAR(120) NOT NULL, upi_id VARCHAR(180) NOT NULL,
 active BOOLEAN NOT NULL, changed_by VARCHAR(180) NOT NULL, changed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_payment_config_history_date ON merchant_payment_config_history(changed_at DESC);

CREATE TABLE payment_detection_events (
 id BIGSERIAL PRIMARY KEY, payment_reference VARCHAR(100), provider VARCHAR(40) NOT NULL,
 delivery_id VARCHAR(150) NOT NULL, event_type VARCHAR(60) NOT NULL,
 amount NUMERIC(18,2), utr VARCHAR(150), settlement_state VARCHAR(40),
 matched BOOLEAN NOT NULL, detail VARCHAR(500), received_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_payment_detection_delivery UNIQUE(provider,delivery_id)
);
CREATE INDEX idx_payment_detection_reference ON payment_detection_events(payment_reference,received_at DESC);
