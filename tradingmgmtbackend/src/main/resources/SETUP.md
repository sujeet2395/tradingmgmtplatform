# Phase 1 — Setup & Integration Guide

This archive contains the **core transaction engine** (Phase 1 of the
delivery plan): entities, repositories, services, controllers, and a
Flyway migration — no chat/AI yet, per the phased plan.

## 1. Copy the source tree

Unzip and copy `src/main/java/com/learn/tradingmgmt/*` and
`src/main/resources/db/migration/V1__init_schema.sql` into your existing
Gradle project at the matching paths. If your main application class
already lives at `com.learn.tradingmgmt.<SomethingApplication>` (or a
parent package of it), Spring Boot's component scan will pick up every
generated `@Service`/`@RestController`/`@Repository` automatically — no
extra `@ComponentScan` needed.

## 2. Add missing Gradle dependencies

Check your `build.gradle` has these (add whichever are missing):

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql' // required for recent Flyway + PostgreSQL
    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

## 3. Database

Create the database and a role, e.g.:

```sql
CREATE DATABASE tradingmgmt;
CREATE USER tradingmgmt WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE tradingmgmt TO tradingmgmt;
```

Flyway will run `V1__init_schema.sql` automatically on startup and create
all 14 Phase 1 tables (see `SCHEMA_DESIGN.md` from the earlier schema
deliverable for the full rationale — this migration is the Phase-1-only
subset of that design: chat/AI/notification tables are deferred to their
own phases).

## 4. Merge `application-snippet.yml`

Copy the keys from `src/main/resources/application-snippet.yml` into your
existing `application.yml`/`application.properties`, adjusting the
datasource credentials.

## 5. What's intentionally NOT included yet

- **Authentication/authorization** — every business command accepts an
  optional `X-User-Id` header for `actingUserId` (used for audit trail
  attribution) instead of a real authenticated principal. Wire in Spring
  Security + role checks (esp. restricting `/api/payments/{id}/verify` to
  `FINANCE`) before this goes anywhere near production data.
- **PAN / bank account encryption** — `Customer.panEncrypted` and
  `bankAccountNumberEncrypted` are plain `byte[]` fields; add a JPA
  `AttributeConverter` that encrypts/decrypts (e.g. AES-GCM with a key
  from your secrets manager) before you ever populate them.
- **Chat, AI extraction, notifications** — Phases 3 and 4 of the delivery
  plan.

## 6. Smoke test — the full order → payment → transfer → settled flow

Once the app is running (`./gradlew bootRun`), this sequence exercises
every state transition end to end:

```bash
BASE=http://localhost:8080/api

# 1. Reference data
COMPANY=$(curl -s -X POST $BASE/companies -H 'Content-Type: application/json' \
  -d '{"legalName":"ABC Ltd","cin":"U12345DL2020PLC000001"}' | jq -r .id)

SECURITY=$(curl -s -X POST $BASE/securities -H 'Content-Type: application/json' \
  -d "{\"companyId\":\"$COMPANY\",\"referencePrice\":1250}" | jq -r .id)

BUYER=$(curl -s -X POST $BASE/customers -H 'Content-Type: application/json' \
  -d '{"fullName":"Buyer One"}' | jq -r .id)

SELLER=$(curl -s -X POST $BASE/customers -H 'Content-Type: application/json' \
  -d '{"fullName":"Seller One"}' | jq -r .id)

# 2. Order lifecycle
ORDER=$(curl -s -X POST $BASE/orders -H 'Content-Type: application/json' \
  -d "{\"orderType\":\"BUY\",\"buyerCustomerId\":\"$BUYER\",\"sellerCustomerId\":\"$SELLER\",\"securityId\":\"$SECURITY\",\"quantity\":2000,\"pricePerShare\":1250}" \
  | jq -r .id)

curl -s -X POST $BASE/orders/$ORDER/verify
curl -s -X POST $BASE/orders/$ORDER/confirm

TXN=$(curl -s $BASE/orders/$ORDER | jq -r .id) # or read transactionId from the confirm response's linked transaction

# 3. Payment (cheque)
PAYMENT=$(curl -s -X POST $BASE/transactions/$TXN/payment -H 'Content-Type: application/json' \
  -d '{"amount":2500000}' | jq -r .id)

curl -s -X POST $BASE/payments/$PAYMENT/cheque-received -H 'Content-Type: application/json' \
  -d '{"chequeNumber":"123456","bankName":"HDFC"}'
curl -s -X POST $BASE/payments/$PAYMENT/deposit
curl -s -X POST $BASE/payments/$PAYMENT/verify

# 4. Share transfer
TRANSFER=$(curl -s -X POST $BASE/transactions/$TXN/transfer | jq -r .id)
curl -s -X POST $BASE/transfers/$TRANSFER/submit
curl -s -X POST $BASE/transfers/$TRANSFER/complete

# 5. Confirm it settled, and inspect the audit trail
curl -s $BASE/transactions/$TXN | jq .status   # → "SETTLED"
curl -s $BASE/audit/Transaction/$TXN | jq .
```

(Note: `TXN` above should be the transaction id returned when the order
was confirmed — wire your test script to capture it from the `confirm`
response once you decide how you want that surfaced, e.g. by adding a
`GET /api/orders/{id}/transaction` convenience endpoint.)
