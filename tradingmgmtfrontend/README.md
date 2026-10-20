# Trading Management Platform — Frontend (Phase 1)

Angular 17 standalone-component app covering the Phase 1 workflow: company/
security/customer master data, order creation and review, and the payment +
share-transfer settlement screens.

## 1. Install

```bash
cd tradingmgmt-frontend
npm install
```

## 2. Point it at your backend

`src/environments/environment.ts` defaults to `http://localhost:8080/api`.
Change it if your backend runs elsewhere.

## 3. Enable CORS on the backend

The backend zip from earlier now includes
`com.learn.tradingmgmt.config.WebConfig`, which allows requests from
`http://localhost:4200` by default. If you changed the frontend's dev port,
override it in `application.yml`:

```yaml
app:
  cors:
    allowed-origins: http://localhost:4200
```

## 4. Run

```bash
npm start   # ng serve — http://localhost:4200
```

## What's here

- **Dashboard** — order counts by status, recent orders table.
- **Companies / Securities / Customers** — simple list + inline create form
  for each, enough master data to build an order.
- **Orders** — list, create form (buyer/seller/security/quantity/price), and
  a detail screen with the exact next action as a button (`Verify order`,
  `Confirm order`, `Reject order`) rather than a generic edit form — status
  drives which buttons are visible, mirroring what the backend actually
  allows next.
- **Transaction detail** — the main settlement screen. Payment and transfer
  are separate cards, each showing only the one next action the current
  status permits (e.g. `Mark cheque received` only appears while the
  payment is `PAYMENT_PENDING`). An audit trail list renders at the bottom
  of both the order and transaction screens, pulled from `/api/audit/...`.

## What's NOT here yet (matches backend Phase 1 scope)

- No login/auth — there's no user switcher, so every action is attributed
  to `null` in the audit trail. Once the backend has real authentication,
  wire the logged-in user's id into an `X-User-Id` header via an
  `HttpInterceptor` (there's a natural seam for this in each service's
  `HttpClient` calls).
- No document upload UI — `/api/documents` exists on the backend but has no
  screen here yet; add one when Phase 1's document workflow needs surfacing.
- No chat/AI screens — those are Phase 3 of the delivery plan.

## Known gap worth knowing about

If a share transfer is rejected, the transaction lands in
`TRANSFER_REJECTED` and the backend's `initiateTransfer` only accepts
transactions in `PAYMENT_VERIFIED` — so there's currently no way to retry a
rejected transfer from either the API or this UI. Decide how you want that
handled (new transfer attempt vs. manual transaction status reset) before
this goes near real trades.
