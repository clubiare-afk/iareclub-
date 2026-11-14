# FundRise

FundRise is a responsive Spring Boot fintech platform with separate user/admin authentication, product management, a two-balance wallet, demo deposits, investments, scheduled rewards, encrypted bank details, withdrawals, transaction history, and admin review workflows. Payments and returns remain simulations until a real provider is integrated.

## Configure Supabase

Create a Supabase project, open **Project Settings > Database**, and set these PowerShell environment variables:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://aws-0-region.pooler.supabase.com:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME="postgres.your-project-ref"
$env:SUPABASE_DB_PASSWORD="your-database-password"
$env:SESSION_COOKIE_SECURE="false"
$env:ADMIN_EMAIL="admin@gmail.com"
$env:ADMIN_PASSWORD="admin@123"
$env:BANK_ENCRYPTION_KEY="a-random-base64-encoded-32-byte-key"
```

The Session pooler is usually easiest on IPv4 networks; follow the port and username shown by Supabase. For HTTPS production, set `SESSION_COOKIE_SECURE=true`. Never commit credentials.

`BANK_ENCRYPTION_KEY` must decode to exactly 32 bytes. Keep it secret and stable: changing it makes existing encrypted bank details unreadable. The included key is development-only and must be overridden in production.

## Development demo payments

Demo payment confirmation is available only when explicitly enabled. The `dev` profile enables it for local testing:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

For staging and production, leave `DEMO_PAYMENTS_ENABLED=false`. Razorpay placeholders (`RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`) are configuration hooks only; this version does not initiate real payments or payouts.

## Run and test

With Java 17+ and Maven 3.9+ installed:

```powershell
mvn test
```

Open `http://localhost:8080`; admin login is at `http://localhost:8080/money`. Flyway applies the financial schema and constraints, while tests use isolated H2 and need no Supabase credentials. Back up an existing database before the first migration. For production, set `JPA_DDL_AUTO=validate`, `SESSION_COOKIE_SECURE=true`, strong admin credentials, and a private encryption key.

## Railway PayU production configuration

Configure these variables in Railway before enabling payments:

```text
PAYU_ENABLED=true
PAYU_ENVIRONMENT=PRODUCTION
PAYU_MERCHANT_KEY=<PayU merchant key>
PAYU_SALT=<PayU salt>
MONEYMINT_PUBLIC_BASE_URL=https://<your-railway-domain>
PAYU_RECONCILIATION_ENABLED=true
```

`MONEYMINT_PUBLIC_BASE_URL` must be the public HTTPS origin only, without a trailing path. The application posts PayU success, failure, and cancellation responses to:

```text
https://<your-railway-domain>/payments/payu/callback
```

Register this additional server callback in PayU Dashboard under **Settings → Webhooks**:

```text
https://<your-railway-domain>/payments/payu/webhook
```

### PayU merchant/display name

PayU Hosted Checkout does not accept a merchant/display-name field in the
payment request. The checkout page and the merchant/payee name shown by UPI or
PayU-generated QR flows come from the PayU merchant profile (and, where
applicable, the acquiring bank/NPCI mapping) associated with
`PAYU_MERCHANT_KEY`. Changing the FundRise application name, `productinfo`, or
callback pages does not change that identity.

After changing the name in the PayU Dashboard, verify that production uses the
key belonging to the updated merchant account. If the old name remains on
Hosted Checkout or in a UPI app, open a PayU support request and ask PayU to
confirm/approve the display-name change and refresh the UPI VPA/QR/acquirer
mapping. Include the merchant ID/key identifier (never the salt), the affected
PayU payment ID, screenshots, and the date the dashboard change was approved.
Existing application transactions and caches do not store the PayU merchant
name.

PayU documents a merchant-details API only for Partner Onboarding accounts. It
requires partner OAuth credentials and `refer_merchant` scope, and it cannot be
used by a standard Hosted Checkout merchant to refresh its own display name.
Do not add partner credentials or expose merchant-profile calls to the browser.

Never commit the merchant key or salt. Rotate any credentials that have previously been committed, then update the Railway variables with the replacements.
