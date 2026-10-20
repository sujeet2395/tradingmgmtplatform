# Trading Management Platform

A back-office web application for tracking **buy/sell transactions of unlisted company shares** end to end: order capture, KYC and document verification, cheque payment confirmation, share transfer, and a full audit trail.

Every financial state change goes through an explicit business command that validates the current status, records an event, and writes an audit entry. Statuses are never edited directly.

> **Status:** Phase 1 (core transaction engine and UI). Chat capture and local-LLM extraction are planned for later phases. See [Roadmap](#roadmap).

---

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [How the workflow works](#how-the-workflow-works)
- [API overview](#api-overview)
- [Deploying to a server](#deploying-to-a-server)
- [Known limitations](#known-limitations)
- [Roadmap](#roadmap)

---

## Features

| Area | What you can do |
|---|---|
| **Master data** | Create and list companies, securities (share classes) and customers (buyers, sellers, brokers). |
| **Orders** | Create BUY/SELL orders, then verify, confirm or reject them. Confirming an order creates its settlement transaction. |
| **Payments** | Initiate a payment, record a received cheque, mark it deposited, and verify it. |
| **Share transfers** | Initiate, submit, complete or reject a transfer once payment is verified. Completing a transfer settles the transaction. |
| **KYC** | Upload KYC documents per customer, verify or reject each one, then mark the customer KYC-verified. Verification requires at least one verified KYC document. |
| **Documents** | Upload PDF/JPG/PNG files against a customer or transaction (cheque evidence, payment proof, transfer forms, agreements). Each upload is versioned and reviewed (Pending, Verified or Rejected). |
| **Audit trail** | Every state change is logged (who, what, old and new value, reason) and shown on the order and transaction screens. |

---

## Architecture

```
┌──────────────┐   /api (proxy)   ┌────────────────────┐        ┌────────────┐
│ Angular UI   │ ───────────────► │ Spring Boot API    │ ─────► │ PostgreSQL │
│ :4200        │                  │ :8080              │        │ :5432      │
└──────────────┘                  │ services + state   │        └────────────┘
                                  │ machine + audit    │
                                  └─────────┬──────────┘
                                            │
                                      ./uploads (files)
```

Design rules:

- **Order and Transaction are separate.** An order is the agreement; a transaction is its settlement (payment and transfer).
- **Current state plus append-only history.** `payments` and `share_transfers` hold the current status; `payment_events`, `transfer_events` and `audit_logs` hold the history.
- **One transition map.** `TransactionServiceImpl` holds the only list of legal transaction status changes.
- **Same-origin API.** The UI calls a relative `/api`. A dev proxy (locally) or nginx (on a server) forwards it to Spring Boot, so no CORS setup is needed.

---

## Tech stack

- **Backend:** Java 17, Spring Boot, Spring Data JPA / Hibernate, Flyway, Bean Validation, Lombok, Gradle
- **Database:** PostgreSQL 14+
- **Frontend:** Angular 17+ (standalone components, no UI framework), TypeScript, RxJS

---

## Repository layout

Suggested monorepo layout:

```
trading-management/
├── backend/                       # Spring Boot (Gradle)
│   └── src/main/
│       ├── java/com/learn/tradingmgmt/
│       │   ├── controller/        # REST endpoints
│       │   ├── service/           # interfaces
│       │   ├── service/impl/      # business logic and state transitions
│       │   ├── repository/        # Spring Data JPA
│       │   ├── entity/            # JPA entities (+ enums/)
│       │   ├── dto/               # request/response DTOs
│       │   ├── exception/         # error handling
│       │   └── config/
│       └── resources/
│           ├── application.yml
│           └── db/migration/      # Flyway: V1, V2, V3...
├── frontend/                      # Angular app
│   ├── proxy.conf.json
│   └── src/app/
│       ├── core/                  # models and HTTP services
│       ├── shared/                # status badge, documents panel
│       └── features/              # dashboard, orders, transactions, customers...
└── README.md
```

---

## Getting started

### Prerequisites

- JDK 17
- Node.js 18+ and npm
- PostgreSQL 14+
- Gradle (or use the project's Gradle wrapper)

### 1. Create the database

```sql
CREATE DATABASE tradingmgmt;
CREATE USER tradingmgmt WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE tradingmgmt TO tradingmgmt;
\c tradingmgmt
GRANT ALL ON SCHEMA public TO tradingmgmt;
```

### 2. Configure and run the backend

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tradingmgmt
    username: tradingmgmt
    password: change-me
  jpa:
    hibernate:
      ddl-auto: validate      # schema is owned by Flyway
    open-in-view: false
  flyway:
    enabled: true
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

app:
  storage:
    dir: ./uploads            # where uploaded documents are stored
```

Start it:

```bash
cd backend
./gradlew bootRun
```

Flyway creates all tables on first start. The API is then at `http://localhost:8080/api`.

### 3. Run the frontend

```bash
cd frontend
npm install
npm start
```

Open **http://localhost:4200**.

The dev server proxies `/api` to `http://localhost:8080` using `proxy.conf.json`:

```json
{
  "/api": { "target": "http://localhost:8080", "secure": false, "changeOrigin": true }
}
```

It is wired in `angular.json` under `serve.options.proxyConfig`. Restart `npm start` after changing it.

### 4. First walkthrough

1. **Companies**: add a company.
2. **Securities**: add a security under that company.
3. **Customers**: add a buyer and a seller. Open each to upload and verify KYC documents.
4. **Orders**: create an order with buyer, seller, security, quantity and price.
5. On the order: **Verify order**, then **Confirm order**, then **Go to settlement**.
6. On the transaction: initiate payment, then mark cheque received, deposited and verify payment.
7. **Initiate transfer**, then submit and complete. The transaction becomes **Settled**.

---

## Configuration

| Setting | Where | Default | Purpose |
|---|---|---|---|
| `spring.datasource.*` | `application.yml` | localhost / tradingmgmt | Database connection |
| `app.storage.dir` | `application.yml` | `./uploads` | Uploaded document storage |
| `spring.servlet.multipart.max-file-size` | `application.yml` | 10MB | Upload size limit |
| `environment.apiBaseUrl` | `frontend/src/environments/environment.ts` | `/api` | API base path |
| `X-User-Id` header | request header | none | Optional user id used for audit attribution (see limitations) |

---

## How the workflow works

**Order status**

```
ORDER_RECEIVED → ORDER_VERIFIED → ORDER_CONFIRMED
        └──────────────┴──→ ORDER_REJECTED
```

**Transaction status** (created when the order is confirmed)

```
ORDER_CONFIRMED → PAYMENT_PENDING → PAYMENT_RECEIVED → PAYMENT_VERIFIED
  → TRANSFER_PENDING → TRANSFER_INITIATED → TRANSFER_COMPLETED → SETTLED
```

Exception branches: `PAYMENT_FAILED`, `TRANSFER_REJECTED`, `CANCELLED`.

**Payment status:** `PAYMENT_PENDING → CHEQUE_RECEIVED → CHEQUE_DEPOSITED → PAYMENT_CONFIRMED`

**Transfer status:** `TRANSFER_PENDING → TRANSFER_SUBMITTED → TRANSFER_COMPLETED` (or `TRANSFER_REJECTED`)

**Document review:** `PENDING → VERIFIED | REJECTED`

The UI only shows the one action the current status allows. The backend enforces the same rules, so an invalid move returns `409 Conflict`.

---

## API overview

Base path: `/api`

| Resource | Endpoints |
|---|---|
| Companies | `GET/POST /companies`, `GET/PUT /companies/{id}` |
| Securities | `GET/POST /securities` (`?companyId=`), `GET /securities/{id}` |
| Customers | `GET/POST /customers`, `GET/PUT /customers/{id}`, `POST /customers/{id}/kyc/verify`, `POST /customers/{id}/kyc/revoke` |
| Orders | `GET/POST /orders` (`?status=`), `GET /orders/{id}`, `POST /orders/{id}/verify`, `/confirm`, `/reject` |
| Transactions | `GET /transactions/{id}`, `GET /transactions/by-order/{orderId}`, `POST /transactions/{id}/payment`, `GET /transactions/{id}/payments`, `POST /transactions/{id}/transfer`, `GET /transactions/{id}/transfers` |
| Payments | `GET /payments/{id}`, `POST /payments/{id}/cheque-received`, `/deposit`, `/verify` |
| Transfers | `GET /transfers/{id}`, `POST /transfers/{id}/submit`, `/complete`, `/reject` |
| Documents | `POST /documents/upload` (multipart), `GET /documents?relatedEntityType=&relatedEntityId=`, `GET /documents/{id}`, `POST /documents/{id}/verify`, `/reject`, `GET /documents/{id}/download`, `POST /documents/{id}/versions` |
| Audit | `GET /audit/{entityType}/{entityId}` |

Errors are returned as JSON: `{ timestamp, status, error, message, path }`.
`404` means not found, `400` a validation error, `409` an invalid state transition.

---

## Deploying to a server

1. Build the frontend: `cd frontend && npm run build`. The output is in `dist/<project>/browser`.
2. Build the backend: `cd backend && ./gradlew bootJar`, then run `java -jar build/libs/*.jar` as a service.
3. Serve the UI with nginx and proxy `/api` to Spring Boot:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    client_max_body_size 10m;

    root /var/www/tradingmgmt;                 # contents of dist/.../browser

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;       # Angular routes
    }
}
```

4. Bind Spring Boot to localhost so it is only reachable through nginx (`server.address: 127.0.0.1`).
5. Put `app.storage.dir` on a persistent, backed-up volume, since uploaded documents live there.
6. Use HTTPS (for example Let's Encrypt) before handling real data.

---

## Known limitations

These are intentional Phase 1 gaps. Address them before using real data.

- **No authentication or roles.** Anyone who can reach the API can perform every action, including verifying payments and KYC. Add Spring Security and restrict payment and document verification to a finance/compliance role.
- **Audit user is empty.** Actions are attributed to whatever `X-User-Id` header is sent, which is normally none.
- **PAN and bank account numbers are not encrypted.** The columns exist but nothing populates or encrypts them. Add encryption before storing them.
- **No KYC gate on orders.** Confirming an order does not check that the buyer and seller are KYC-verified, and verifying a payment does not require verified cheque evidence.
- **A rejected transfer cannot be retried.** The transaction stays in `TRANSFER_REJECTED`, and a new transfer can only start from `PAYMENT_VERIFIED`.
- **Some endpoints return entities, not DTOs.** The orders list uses a response DTO; others still return JPA entities (eager-fetched). Convert them as data grows.
- **Local file storage.** Uploads are written to disk, which does not work for multiple server instances. Move to object storage (S3) when scaling out.
- **Order and transaction numbers** come from a row count, which can collide under concurrent requests. Switch to a database sequence.

---

## Roadmap

- **Phase 2:** authentication, role-based access, KYC gating, dashboard widgets, notifications.
- **Phase 3:** chat capture with a lightweight local LLM that converts unstructured messages into structured draft orders for human review. The AI only proposes; backend services still own every state change.
- **Phase 4:** reminders and automation, WhatsApp channel.
- **Phase 5:** model evaluation on labelled conversations, monitoring, performance tuning.

Securities, payment and KYC/AML rules apply to this business. Have the final workflow reviewed against the applicable regulations before going live.
