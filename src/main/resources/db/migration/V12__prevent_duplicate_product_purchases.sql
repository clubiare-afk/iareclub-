CREATE OR REPLACE FUNCTION prevent_duplicate_product_purchase()
RETURNS trigger AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(NEW.user_id, NEW.product_id::INTEGER);
    IF EXISTS (
        SELECT 1 FROM user_investments
        WHERE user_id = NEW.user_id AND product_id = NEW.product_id
    ) THEN
        RAISE EXCEPTION 'Product already purchased by this user'
            USING ERRCODE = 'unique_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS user_investments_one_purchase_per_product ON user_investments;
CREATE TRIGGER user_investments_one_purchase_per_product
BEFORE INSERT ON user_investments
FOR EACH ROW EXECUTE FUNCTION prevent_duplicate_product_purchase();
