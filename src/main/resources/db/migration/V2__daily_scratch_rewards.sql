CREATE TABLE IF NOT EXISTS scratch_rewards (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id), game_day DATE NOT NULL,
 amount NUMERIC(18,2) NOT NULL CHECK(amount >= 1 AND amount <= 50), transaction_reference VARCHAR(100) NOT NULL UNIQUE,
 scratched_at TIMESTAMPTZ NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'SUCCESSFUL',
 CONSTRAINT uk_scratch_user_game_day UNIQUE(user_id,game_day)
);
CREATE INDEX IF NOT EXISTS idx_scratch_user_date ON scratch_rewards(user_id,scratched_at DESC);
