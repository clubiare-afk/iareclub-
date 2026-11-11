CREATE TABLE IF NOT EXISTS app_users (
 id BIGSERIAL PRIMARY KEY, full_name VARCHAR(100) NOT NULL, email VARCHAR(180) NOT NULL,
 password_hash VARCHAR(100) NOT NULL, role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER', created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_app_users_email UNIQUE(email)
);
CREATE TABLE IF NOT EXISTS investment_products (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(120) NOT NULL, image_url VARCHAR(1000) NOT NULL,
 original_price NUMERIC(14,2) NOT NULL, discount_price NUMERIC(14,2) NOT NULL,
 daily_earnings NUMERIC(14,2) NOT NULL, total_days INTEGER NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS wallets (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES app_users(id),
 deposit_balance NUMERIC(18,2) NOT NULL DEFAULT 0, reward_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
 total_invested NUMERIC(18,2) NOT NULL DEFAULT 0, total_rewards_earned NUMERIC(18,2) NOT NULL DEFAULT 0,
 total_withdrawn NUMERIC(18,2) NOT NULL DEFAULT 0, pending_withdrawals NUMERIC(18,2) NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 0, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS wallet_transactions (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id), type VARCHAR(40) NOT NULL,
 status VARCHAR(20) NOT NULL, amount NUMERIC(18,2) NOT NULL, balance_after NUMERIC(18,2),
 reference VARCHAR(100) NOT NULL UNIQUE, description VARCHAR(250) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_user_date ON wallet_transactions(user_id,created_at DESC);
CREATE TABLE IF NOT EXISTS deposits (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id), amount NUMERIC(18,2) NOT NULL,
 payment_reference VARCHAR(100) NOT NULL UNIQUE, payment_method VARCHAR(30) NOT NULL,
 status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL, completed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_deposit_user_date ON deposits(user_id,created_at DESC);
CREATE INDEX IF NOT EXISTS idx_deposit_status ON deposits(status);
CREATE TABLE IF NOT EXISTS user_investments (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id), product_id BIGINT NOT NULL REFERENCES investment_products(id),
 product_name VARCHAR(120) NOT NULL, product_image_url VARCHAR(1000) NOT NULL,
 investment_amount NUMERIC(18,2) NOT NULL, daily_reward NUMERIC(18,2) NOT NULL, total_days INTEGER NOT NULL,
 projected_returns NUMERIC(18,2) NOT NULL, rewards_collected INTEGER NOT NULL DEFAULT 0,
 total_rewards_collected NUMERIC(18,2) NOT NULL DEFAULT 0, next_reward_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL, request_reference VARCHAR(100) NOT NULL UNIQUE, purchased_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_investment_user_status ON user_investments(user_id,status);
CREATE TABLE IF NOT EXISTS daily_rewards (
 id BIGSERIAL PRIMARY KEY, investment_id BIGINT NOT NULL REFERENCES user_investments(id), user_id BIGINT NOT NULL REFERENCES app_users(id),
 reward_number INTEGER NOT NULL, amount NUMERIC(18,2) NOT NULL, collected_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_reward_investment_number UNIQUE(investment_id,reward_number)
);
CREATE INDEX IF NOT EXISTS idx_reward_user_date ON daily_rewards(user_id,collected_at DESC);
CREATE TABLE IF NOT EXISTS bank_accounts (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES app_users(id), account_holder_name VARCHAR(100) NOT NULL,
 bank_name VARCHAR(100) NOT NULL, account_number_encrypted VARCHAR(600) NOT NULL, account_last_four VARCHAR(4) NOT NULL,
 ifsc_encrypted VARCHAR(300) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS withdrawal_requests (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id), bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
 amount NUMERIC(18,2) NOT NULL, reference VARCHAR(100) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL,
 requested_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, payout_reference VARCHAR(120),
 admin_remarks VARCHAR(500), funds_released BOOLEAN NOT NULL DEFAULT FALSE, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_withdrawal_user_date ON withdrawal_requests(user_id,requested_at DESC);
CREATE INDEX IF NOT EXISTS idx_withdrawal_status ON withdrawal_requests(status);
CREATE TABLE IF NOT EXISTS admin_audit_logs (
 id BIGSERIAL PRIMARY KEY, admin_identity VARCHAR(180) NOT NULL, entity_type VARCHAR(50) NOT NULL,
 entity_id BIGINT NOT NULL, action VARCHAR(80) NOT NULL, previous_status VARCHAR(30), new_status VARCHAR(30),
 remarks VARCHAR(500), created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON admin_audit_logs(entity_type,entity_id);
