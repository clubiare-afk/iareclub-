# ZapUPI deployment checklist

The integration uses the official PayIn endpoints:

- `POST https://pay.zapupi.com/api/create-order`
- `POST https://pay.zapupi.com/api/order-status`
- Webhook: `POST https://www.fundrise.shop/payments/zapupi/webhook`
- Browser return: `GET https://www.fundrise.shop/payments/zapupi/return`

## Railway variables

Configure these as Railway service variables. Never put the API key in source, browser code, build output, or logs.

```text
ZAPUPI_API_KEY=<dashboard API key>
ZAPUPI_ENABLED=true
ZAPUPI_LIVE_MODE=true
ZAPUPI_RECONCILIATION_ENABLED=true
ZAPUPI_PUBLIC_BASE_URL=https://www.fundrise.shop
PAYMENT_ALLOWED_RETURN_ORIGINS=https://www.fundrise.shop,https://fundrise.shop
```

`ZAPUPI_PUBLIC_BASE_URL` is the canonical public HTTPS origin used in every newly-created ZapUPI order. For local testing it must be the current ngrok HTTPS origin (without a trailing slash), and that origin must also be included in `PAYMENT_ALLOWED_RETURN_ORIGINS`. Restart FundRise after changing it; an already-created order retains its original callback URL and QR. For production use `https://www.fundrise.shop`.

Browser returns and webhooks deliberately use different origins. The per-transaction `success_url`, `failed_url`, and `timeout_url` use the validated origin where the user started payment (localhost, an allowlisted ngrok origin, or production). Only `webhook_url` uses `ZAPUPI_PUBLIC_BASE_URL`, because ZapUPI's server must be able to reach it. Never set a localhost webhook URL.

For local/test use `ZAPUPI_ENABLED=true` and `ZAPUPI_LIVE_MODE=false`. Example ngrok values:

```text
ZAPUPI_PUBLIC_BASE_URL=https://your-current-domain.ngrok-free.app
PAYMENT_ALLOWED_RETURN_ORIGINS=http://localhost:8080,https://your-current-domain.ngrok-free.app
SESSION_COOKIE_SECURE=false
SESSION_COOKIE_SAME_SITE=lax
```

Set the dashboard webhook to `https://your-current-domain.ngrok-free.app/payments/zapupi/webhook` while testing. Confirm it is reachable from the internet before paying. After test-mode orders, redirects, failed/pending states, delayed webhooks, and duplicate webhook deliveries have been verified, set `ZAPUPI_LIVE_MODE=true` in Railway. Live mode rejects any verified result whose environment is not `cashier`.

For Railway/production use `SESSION_COOKIE_SECURE=true`, `SESSION_COOKIE_SAME_SITE=lax`, set `MONEY_MINT_SITE_ORIGIN` to the exact canonical production origin, and include that exact origin in `PAYMENT_ALLOWED_RETURN_ORIGINS`. `fundrise.shop` and `fundraise.shop` are different domains; configure the one actually serving FundRise rather than relying on a spelling variant.

Enable ZapUPI's **Custom API Feature** in the dashboard to receive `payment_data` and display a direct UPI-app QR. If the feature is disabled, FundRise displays a QR and button that open ZapUPI's official hosted Cashier; it never invents a UPI payment payload.

In the ZapUPI dashboard select Cashier mode and configure the webhook URL above. ZapUPI's published webhook server IP is `72.61.225.127`. If an external firewall/WAF is used, allow inbound HTTPS webhook traffic from `72.61.225.127/32`. Railway normally manages its public ingress and does not provide an application-level source-IP firewall rule. Do not enforce the source IP from an untrusted `X-Forwarded-For` header inside Spring; keep the authenticated order-status verification as the payment authority.

## Webhook security limitation

ZapUPI's public documentation (reviewed 2026-11-16) documents no webhook signature, secret, timestamp, or delivery ID. Therefore the webhook body is never treated as payment proof. It only supplies the merchant order ID; the backend then calls the authenticated `order-status` endpoint and validates order ID, original amount, paid status, paid amount, environment, transaction ID, and UTR before crediting. Ask ZapUPI support for a documented signature scheme and add it when available.

## Cashier automatic-verification limitation

FundRise never asks the customer for a UTR. ZapUPI Cashier may still display its own UTR form when the merchant account/payment route cannot automatically match the bank transaction. FundRise cannot remove or bypass controls on the provider-hosted page. In observed live responses, affected orders returned `status=Failed`, `pay_amount` equal to the order amount, a transaction ID, and no UTR; successful orders returned `status=Success` with a UTR. FundRise keeps the former in `PENDING/AWAITING_SETTLEMENT`, continues reconciliation, and does not credit or invite a duplicate payment. Ask ZapUPI support to enable an automatic-confirmation Cashier/ZapPay route for the merchant account if no-UTR checkout is required.

## Rollback and records

Migration `V23__generic_gateway_transaction_ids.sql` renames the old PayU-specific transaction-ID column without deleting any deposit, wallet, ledger, or history row. Old records remain visible as legacy gateway records. Take a database backup and test the Flyway migration against a production schema clone before deployment.
