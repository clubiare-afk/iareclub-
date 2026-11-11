CREATE TABLE carousel_banners (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_carousel_banner_order CHECK (display_order >= 0)
);

CREATE INDEX idx_carousel_banners_active_order
    ON carousel_banners (active, display_order, id);
