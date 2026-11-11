ALTER TABLE wallet_transactions
    DROP CONSTRAINT IF EXISTS wallet_transactions_type_check;

ALTER TABLE wallet_transactions
    ADD CONSTRAINT wallet_transactions_type_check CHECK (
        type IN (
            'DEPOSIT',
            'ADMIN_CREDIT',
            'INVESTMENT_PURCHASE',
            'DAILY_REWARD',
            'SCRATCH_REWARD',
            'REFERRAL_BONUS',
            'REFERRAL_REVERSAL',
            'WITHDRAWAL_REQUEST',
            'WITHDRAWAL_SUCCESS',
            'WITHDRAWAL_REFUND'
        )
    );
