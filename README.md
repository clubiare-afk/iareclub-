# Money Mint

Money Mint is a responsive Spring Boot fintech platform with separate user/admin authentication, product management, a two-balance wallet, demo deposits, investments, scheduled rewards, encrypted bank details, withdrawals, transaction history, and admin review workflows. Payments and returns remain simulations until a real provider is integrated.

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
