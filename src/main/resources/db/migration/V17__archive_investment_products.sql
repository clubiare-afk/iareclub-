ALTER TABLE investment_products
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_investment_products_catalogue
    ON investment_products (archived, active, created_at DESC);
