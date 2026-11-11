CREATE OR REPLACE FUNCTION prevent_duplicate_product_purchase()
RETURNS trigger AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.user_id::TEXT || ':' || NEW.product_id::TEXT, 0));
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
