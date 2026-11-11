-- Cover the most frequent filtered lists and preserve sort order in PostgreSQL.
CREATE INDEX IF NOT EXISTS idx_products_active_created
    ON investment_products(active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_deposit_status_created
    ON deposits(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_withdrawal_status_requested
    ON withdrawal_requests(status, requested_at DESC);
CREATE INDEX IF NOT EXISTS idx_investment_user_purchased
    ON user_investments(user_id, purchased_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_user_type_status_date
    ON wallet_transactions(user_id, type, status, created_at DESC);
