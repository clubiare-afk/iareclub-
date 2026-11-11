CREATE TABLE IF NOT EXISTS scratch_cooldowns (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES app_users(id),
    next_available_at TIMESTAMPTZ NOT NULL
);
