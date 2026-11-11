ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS balance_before NUMERIC(18,2);
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30);
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS admin_identity VARCHAR(180);

CREATE TABLE admin_credit_audits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id),
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method = 'ADMIN_CREDIT'),
    status VARCHAR(20) NOT NULL CHECK (status = 'SUCCESSFUL'),
    balance_before NUMERIC(18,2) NOT NULL,
    balance_after NUMERIC(18,2) NOT NULL,
    admin_identity VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_admin_credit_audit_user_date ON admin_credit_audits(user_id, created_at DESC);

CREATE OR REPLACE FUNCTION reject_admin_credit_audit_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Admin credit audit records are immutable';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS admin_credit_audits_immutable ON admin_credit_audits;
CREATE TRIGGER admin_credit_audits_immutable
BEFORE UPDATE OR DELETE ON admin_credit_audits
FOR EACH ROW EXECUTE FUNCTION reject_admin_credit_audit_mutation();
