ALTER TABLE app_users ADD COLUMN IF NOT EXISTS referral_code VARCHAR(16);
CREATE UNIQUE INDEX IF NOT EXISTS uk_app_users_referral_code ON app_users(referral_code) WHERE referral_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS referrals (
 id BIGSERIAL PRIMARY KEY,
 referrer_id BIGINT NOT NULL REFERENCES app_users(id),
 referred_user_id BIGINT NOT NULL REFERENCES app_users(id),
 qualifying_investment_id BIGINT REFERENCES user_investments(id),
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 bonus_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
 referral_code_used VARCHAR(16) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 qualified_at TIMESTAMPTZ,
 reversed_at TIMESTAMPTZ,
 reversal_reason VARCHAR(300),
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_referral_referred_user UNIQUE(referred_user_id),
 CONSTRAINT uk_referral_qualifying_investment UNIQUE(qualifying_investment_id),
 CONSTRAINT ck_referral_not_self CHECK(referrer_id <> referred_user_id),
 CONSTRAINT ck_referral_status CHECK(status IN ('PENDING','SUCCESSFUL','REVERSED'))
);
CREATE INDEX IF NOT EXISTS idx_referral_referrer_status ON referrals(referrer_id,status);
CREATE INDEX IF NOT EXISTS idx_referral_created_at ON referrals(created_at DESC);
