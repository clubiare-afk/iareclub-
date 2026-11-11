CREATE UNIQUE INDEX uk_deposits_payu_payment_id
    ON deposits (payu_payment_id)
    WHERE payu_payment_id IS NOT NULL;
