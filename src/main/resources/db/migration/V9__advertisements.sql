CREATE TABLE advertisements (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_advertisement_image_url CHECK (image_url ~ '^https?://')
);

CREATE INDEX idx_advertisements_active_updated
    ON advertisements(active, updated_at DESC);
