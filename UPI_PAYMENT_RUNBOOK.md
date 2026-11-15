# Direct UPI wallet payments

## Safety status

The QR checkout, payment records, polling, audit trail, and exactly-once wallet credit path are implemented. Automatic credit is **off by default**. Do not enable it for YouPI-PG SMS, Gmail, Android-notification, browser, screenshot, or customer-supplied UTR events.

YouPI-PG describes itself as an experimental research project. Its alert observations are not bank settlement proof. In this application, signed YouPI events only move a request to `DETECTED_UNVERIFIED`; its operator-imported reconciliation moves it to `REQUIRES_REVIEW`. Neither credits a wallet. The repository currently declares a commercial license; obtain and review the actual license terms before deploying its code.

## Local run

Use Java 17+ and PostgreSQL. Set the environment variables below, then run:

```powershell
cd club
./mvnw spring-boot:run
```

Flyway migration `V21__upi_wallet_payments.sql` creates the new tables without deleting or rewriting PayU deposits.

Sign in as an administrator and open `/admin/payment-settings`. Save the merchant display name and receiving UPI ID. Activate the configuration only after checking the UPI ID. Set `UPI_PAYMENTS_ENABLED=true` to allow users to create payment requests.

## Railway variables

Required application variables:

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`
- `BANK_ENCRYPTION_KEY` (base64-encoded 32-byte AES key)
- `PAYMENTS_CHECKOUT=UPI`
- `UPI_PAYMENTS_ENABLED=true` after admin configuration is ready
- existing `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` when support uploads are used

Optional rollout/fallback variables:

- `PAYMENTS_CHECKOUT=PAYU`, `PAYU_ENABLED=true`, `PAYU_MERCHANT_KEY`, `PAYU_SALT` restore legacy checkout; history is always preserved.
- `YOUPI_WEBHOOK_SECRET` authenticates YouPI signals at `POST /payments/webhooks/youpi`. It does not authorize wallet credit.

Authorized verification-provider variables:

- `AUTHORIZED_PAYMENT_PROVIDER_WEBHOOK_SECRET`
- `AUTHORIZED_PAYMENT_PROVIDER_AUTO_CREDIT=true` only after a bank/acquirer-authorized provider contract and API have been independently validated

The authorized webhook is `POST /payments/webhooks/authorized-provider`. It requires HMAC-SHA256 headers `X-Provider-Signature`, `X-Provider-Delivery-Id`, and optionally the timestamp-bound `X-Provider-Signature-V2` plus `X-Provider-Timestamp`. A `PAYMENT_VERIFIED` body must contain `reference`, exact `amount`, unique `transactionId`, `utr`, and the exact `merchantUpiId`. Adapt the field mapping only to the selected provider's documented, signed contract.

## Verification and real-payment test

1. Keep authorized automatic credit disabled.
2. Create a ₹1 request from a test user and scan the generated QR.
3. Confirm the payee name and receiving UPI ID in the UPI app before paying.
4. Confirm receipt in an official bank statement or an authorized merchant API—not an SMS, email, notification, screenshot, or user-entered UTR.
5. In `/admin/payments`, record the provider transaction ID, verified UTR, statement date/reference, and reviewer note. The wallet ledger and audit log are written transactionally.
6. Repeat the review request and confirm the wallet balance does not increase again.
7. Enable automatic credit only after mocked signature, replay, wrong amount, wrong merchant, delayed delivery, and duplicate-delivery tests pass against the real provider contract.

## Remaining external work

The receiving bank or an authorized merchant/acquirer must supply production onboarding, credentials, signing keys, transaction-status/webhook documentation, settlement/reconciliation access, and permission for the intended business model. No such bank API is supplied by YouPI-PG, so this repository intentionally cannot claim automatic bank verification today.

## Actual YouPI-PG service integration

The reviewed upstream source is vendored under `integrations/youpi-pg`. The Spring application uses YouPI's documented interfaces rather than reimplementing its state machine:

- `POST /api/v1/payments` with `X-Merchant-Id`, backend-only `X-Merchant-Key`, and `Idempotency-Key` creates the session.
- Customers are redirected to the returned YouPI `checkoutUrl`, where YouPI generates its dynamic QR, UPI intent, timers, queue, and SSE updates.
- `POST /api/v1/admin/upi-settings` synchronizes the administrator's active UPI ID and payee name using `ADMIN_API_TOKEN`.
- `POST /payments/webhooks/youpi` receives raw-body HMAC-signed `OBSERVED_SUCCESS`, `MANUAL_REVIEW`, `RECONCILED`, `FAILED`, `EXPIRED`, and `CANCELLED` outcomes.

Set these Spring/Railway variables:

- `YOUPI_INTEGRATION_ENABLED=true`
- `YOUPI_API_BASE_URL` — private Railway URL for the YouPI payment API, such as `http://youpi-pg.railway.internal:6100`
- `YOUPI_MERCHANT_ID`
- `YOUPI_MERCHANT_KEY`
- `YOUPI_ADMIN_TOKEN`
- `YOUPI_WEBHOOK_SECRET` — the same merchant webhook secret configured in YouPI

The local QR page is only a development fallback when `YOUPI_INTEGRATION_ENABLED=false`; it is not represented as the upstream gateway.

## Railway YouPI service

Create a second Railway service rooted at `integrations/youpi-pg` and deploy its upstream `Dockerfile`. Attach a persistent volume at `/app/data` if SQLite fallback is used, or preferably configure a backed-up PostgreSQL `DATABASE_URL`. Configure at least:

- `NODE_ENV=production`, `YOUPI_MODE=production`
- `DATABASE_URL`, `USE_SQLITE_FALLBACK=false` for PostgreSQL
- `ENCRYPTION_MASTER_KEY` (64 hex characters), `JWT_SECRET` (32+ characters)
- `ADMIN_API_TOKEN`, `OBSERVER_API_TOKEN`
- `CHECKOUT_PUBLIC_URL` and `PUBLIC_APP_URL` using the public HTTPS YouPI domain
- `YOUPI_ALLOWED_RETURN_ORIGINS=https://www.fundrise.shop`
- `CHECKOUT_FRAME_ANCESTORS=https://www.fundrise.shop`
- the observer variables documented in upstream `docs/OBSERVERS_SETUP.md`

Expose the customer checkout through HTTPS. Keep the payment API and admin endpoints private where Railway networking permits; the Spring service still authenticates every call. Configure the health check as `/health` on port 6100.

The Android observer cannot run on Railway. Install it on an always-on Android device containing the receiving account's alert source and enroll it using YouPI's documented pairing process. Gmail observation may run as a separate service, but it needs OAuth credentials and persistent encrypted token storage. Both channels produce observation evidence only.

For safe reconciliation, download an official bank statement and import it through YouPI's statement-reconciliation workflow. A signed `RECONCILED` webhook records `REQUIRES_REVIEW` in FundRise; an authorized administrator then verifies the statement evidence before the wallet is credited.

Upstream currently declares `"license": "Commercial"` in `package.json` but the cloned revision contains no `LICENSE` file. Confirm usage and redistribution rights with the maintainer before production deployment.
