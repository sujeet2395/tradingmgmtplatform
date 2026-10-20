# Trading Management Platform — Implementation Plan

> Local LLM–assisted chat-to-structured-order system for tracking unlisted share
> buy/sell transactions, payment confirmation, and share transfer.
>
> This document is written for consumption by an AI coding agent (e.g. GitHub
> Copilot / Copilot Workspace) to scaffold and implement the system phase by
> phase. Each section is self-contained enough to drive a single implementation
> task.

---

## 0. Reference Context

This plan formalizes a design worked out in an earlier ChatGPT conversation
about converting unstructured sales/service chat into structured orders using
a small local LLM, then extended to an unlisted-share trading platform with
cheque-based payment confirmation and share-transfer tracking. Key principle
carried forward from that discussion:

> **AI interprets the conversation. Spring Boot controls the transaction.**
> The AI must never independently mark a payment as received, initiate a
> transfer, or change a financial status. It only produces structured
> *claims/events* that a human or a deterministic business rule must verify.

(Note: this is a paraphrase of a prior AI conversation supplied by the user,
not a citation of any external document — treat it purely as design input,
not as fact to double-check.)

---

## 1. Business Objective

Build a platform that manages the full lifecycle of a **BUY/SELL share order**
captured through natural-language chat (web chat, and later WhatsApp/other
channels):

```
Buyer/Seller Chat → AI Extraction → Structured Order (draft)
   → Human Review → Order Confirmed → Payment (Cheque) → Payment Verified
   → Share Transfer → Transfer Completed → Settled
```

A lightweight **local LLM** (no cloud calls, no customer chat leaves the
server) converts unstructured chat into a structured JSON object matching a
strict schema. The Spring Boot backend owns all business rules, financial
state transitions, and audit trail — the AI is advisory only.

---

## 2. Tech Stack

| Layer | Choice |
|---|---|
| Backend | Java 21+, Spring Boot, Spring Security, Spring Data JPA, Hibernate |
| Database | PostgreSQL, Flyway migrations |
| Frontend | Angular, TypeScript, Angular Material, RxJS, Reactive Forms |
| AI/LLM service | Colocated on the same server as the Spring Boot backend (see §8) |
| AI runtime | Ollama or llama.cpp, quantized 1B–4B instruct model |
| AI service framework | Python + FastAPI, internal-only REST endpoint |
| API docs | OpenAPI/Swagger |
| Build | Maven or Gradle |

---

## 3. High-Level Architecture

```
                 ┌─────────────┐
                 │  Angular UI │
                 └──────┬──────┘
                        │ REST (JWT)
                 ┌──────▼──────────────┐
                 │   Spring Boot API    │
                 │  (single deployment  │
                 │   host, see §8)      │
                 └──────┬───────┬──────┘
                        │       │ localhost/internal HTTP
          ┌─────────────┘       └─────────────┐
          ▼                                    ▼
┌───────────────────┐               ┌─────────────────────┐
│ Transaction Engine │               │  AI Extraction       │
│  Order/Payment/     │               │  Service (FastAPI +  │
│  Transfer/Audit     │               │  Ollama/llama.cpp,    │
└─────────┬──────────┘               │  same host)          │
          │                          └──────────┬───────────┘
          ▼                                     │
   ┌─────────────┐                              │
   │ PostgreSQL   │◄─────── structured JSON ─────┘
   └─────────────┘        (validated, human-reviewed)
```

Key rule: the AI service has **no direct database access**. It only receives
chat text and returns JSON; the Spring Boot backend validates and persists it
after human review.

---

## 4. Core Business Entities

- `User` (roles: ADMIN, OPERATIONS, DEALER, COMPLIANCE, FINANCE, VIEWER)
- `Customer` (buyer/seller/broker, KYC fields, PAN, bank details)
- `Company` (legal name, CIN, status)
- `Security` (belongs to Company; ISIN, face value, share class)
- `Order` — the commercial intention (BUY/SELL, quantity, price, parties)
- `Transaction` — the actual settlement process linked to an Order
- `TransactionParty`
- `Payment` + `PaymentEvent` (cheque-based, extensible to other methods)
- `ShareTransfer` + `ShareTransferEvent`
- `ChatConversation` + `ChatMessage`
- `AIExtraction` + `AIEvent` (raw AI output, pre-review)
- `Document` + `DocumentVersion` (KYC, cheque evidence, transfer forms)
- `AuditLog` (immutable, on every state-changing action)
- `Notification`

**Order vs Transaction** are kept distinct: an Order is the agreement: a
Transaction is its settlement (payment + transfer). This separation makes
partial fills, renegotiation, and multiple transactions per conversation
possible later.

---

## 5. Database Schema (starting point)

```
users
customers
companies
securities

orders
transactions
transaction_parties

payments
payment_events

share_transfers
transfer_events

chat_conversations
chat_messages
ai_extractions
ai_events

documents
document_versions

audit_logs
notifications
```

Conventions:
- UUID primary keys
- `created_at`, `updated_at`, `created_by`, `updated_by` on every table
- Optimistic locking (`@Version`) on Order, Transaction, Payment, ShareTransfer
- No hard deletes on financial records — soft-delete/archive only
- Flyway migration per schema change, versioned

---

## 6. State Machines

### 6.1 Transaction lifecycle

```
DRAFT → ORDER_RECEIVED → ORDER_VERIFIED → ORDER_CONFIRMED
      → PAYMENT_PENDING → PAYMENT_RECEIVED → PAYMENT_VERIFIED
      → TRANSFER_PENDING → TRANSFER_INITIATED → TRANSFER_COMPLETED
      → SETTLED
```
Exception branches: `PAYMENT_FAILED`, `TRANSFER_REJECTED` — each must be an
explicit, reachable state, not just an error code.

### 6.2 Payment status (cheque-first, extensible)

```
PAYMENT_PENDING → CHEQUE_RECEIVED → CHEQUE_DEPOSITED → CHEQUE_CLEARED
                → PAYMENT_CONFIRMED
              (or → PAYMENT_FAILED / PAYMENT_CANCELLED)
```

### 6.3 Share transfer status

```
TRANSFER_PENDING → DOCUMENTS_PENDING → DOCUMENTS_VERIFIED
                 → TRANSFER_INITIATED → TRANSFER_SUBMITTED
                 → TRANSFER_IN_PROCESS → TRANSFER_COMPLETED
              (or → TRANSFER_REJECTED)
```

Implementation rule: **status fields are never directly settable via a generic
PATCH/PUT.** Every transition goes through a named business command
(`/verify`, `/deposit`, `/confirm`, `/submit`, `/complete`, etc.), each of
which validates the current state before transitioning and writes an
`AuditLog` entry.

---

## 7. AI Extraction Contract

### 7.1 What the AI is allowed to do
- Read chat message(s)
- Extract entities into a strict schema
- Flag missing fields
- Emit **claims**, never confirmations, for anything financial

### 7.2 What the AI must never do
- Set `PAYMENT_CONFIRMED` or `TRANSFER_COMPLETED` directly
- Invent a value it wasn't given (must return `null` + add to
  `missing_information`)
- Write to the database directly

### 7.3 Output schema

```json
{
  "intent": "CREATE_ORDER",
  "transaction_type": "BUY",
  "entities": {
    "company": null,
    "security": null,
    "buyer": null,
    "seller": null,
    "quantity": null,
    "price_per_share": null,
    "payment_method": null,
    "payment_claim": null,
    "transfer_claim": null
  },
  "missing_information": [],
  "events": [],
  "confidence": 0.0,
  "requires_human_review": true
}
```

Example — a message like *"Cheque deposited today"* must produce a
`PAYMENT_CLAIMED` event, distinct from the backend-controlled
`PAYMENT_VERIFIED` status. Same for *"shares transferred"* → `TRANSFER_CLAIMED`,
never `TRANSFER_COMPLETED`.

### 7.4 Review workflow

```
Chat → AI Extraction → Schema Validation → Human Review (Approve/Edit/Reject)
     → Business Command → Transaction Service → PostgreSQL
```

Every approve/edit/reject action is audited.

---

## 8. AI Service Deployment (colocated with backend)

Since the AI extraction service runs **on the same host as Spring Boot**:

- Run it as a separate OS process (FastAPI + uvicorn), reachable only on
  `localhost` — do not expose it externally; the Spring Boot backend is the
  only caller.
- Model choice: quantized instruct model in the **1B–4B parameter range**
  (e.g. via Ollama), sized to fit alongside the JVM's memory footprint on the
  same box — right-size the host's RAM to cover both the JVM heap and the
  model's resident memory before picking a specific model checkpoint.
- Treat the AI process like any other internal dependency: give it a health
  check endpoint, restart policy (systemd or Docker), and its own log stream
  separate from the Spring Boot logs.
- Because both services share CPU/RAM with a single host, load-test
  concurrent chat extraction against normal backend traffic before going to
  production, and revisit moving the AI service to a separate host if
  contention shows up.
- Internal-only endpoints: `POST /internal/ai/extract`,
  `POST /internal/ai/classify`, `POST /internal/ai/validate`.

---

## 9. REST API (Spring Boot)

```
POST   /api/chats
GET    /api/chats
GET    /api/chats/{id}
POST   /api/chats/{id}/messages

POST   /api/orders
GET    /api/orders
GET    /api/orders/{id}
POST   /api/orders/{id}/confirm

GET    /api/transactions/{id}

POST   /api/transactions/{id}/payment
POST   /api/payments/{id}/cheque-received
POST   /api/payments/{id}/deposit
POST   /api/payments/{id}/verify

POST   /api/transactions/{id}/transfer
POST   /api/transfers/{id}/submit
POST   /api/transfers/{id}/complete

GET    /api/audit/{entityType}/{entityId}

# internal only, called by Spring Boot -> AI service
POST   /internal/ai/extract
POST   /internal/ai/classify
POST   /internal/ai/validate
```

---

## 10. Angular UI Plan

- **Dashboard** — open orders, pending payments, cheque pending, transfers
  pending, completed/rejected counts
- **Chat inbox** — conversation list with AI status + order status columns
- **Conversation detail** — chat on the left, live AI extraction panel on the
  right
- **AI extraction review** — Approve / Edit / Reject / Create Order
- **Order list / detail**
- **Transaction detail**
- **Payment management** — cheque verification screen
- **Share-transfer management**
- **Document management** — versioned, no overwrites
- **Audit timeline** — per-entity, chronological, human-readable
- **Administration** — users, roles, companies/securities

---

## 11. Security & Audit

- JWT-based auth, Spring Security, role-based authorization
- Roles: `ADMIN`, `OPERATIONS`, `DEALER`, `COMPLIANCE`, `FINANCE`, `VIEWER`
  — only FINANCE can verify payments, only OPERATIONS/COMPLIANCE can move
  transfer status forward
- Encrypt sensitive customer data (PAN, bank details) at rest
- Every state-changing action writes an immutable `AuditLog` row: who, what,
  entity type/id, old value, new value, timestamp, reason, source
- Idempotency keys on payment and transfer commands to prevent duplicates
- Input validation (Bean Validation) on every write endpoint

---

## 12. Phased Delivery Plan

**Phase 1 — Core transaction engine (no AI yet)**
Users/roles, Company/Security, Order, Transaction, Payment, ShareTransfer,
Document, AuditLog. Prove the manual workflow end-to-end first.

**Phase 2 — Angular UI for Phase 1**
Dashboard, order/payment/transfer management, document upload, audit
timeline.

**Phase 3 — Chat + AI extraction**
ChatConversation/ChatMessage, FastAPI + local LLM service (colocated),
structured JSON extraction, AI extraction review screen.

**Phase 4 — Automation**
Missing-information detection, payment/transfer reminders, notifications,
document validation rules.

**Phase 5 — Additional channels & hardening**
WhatsApp Business API as an additional `ChatConversation` source (webhook →
Spring Boot → same chat pipeline), AI model evaluation against a labeled
conversation dataset, performance tuning, monitoring.

For each phase, produce: DB migration, entities, DTOs, repositories,
services, controllers, validation, security rules, tests, Angular
components/services, and updated API docs.

---

## 13. Non-Negotiable Business Rules

1. AI cannot confirm payment.
2. AI cannot confirm share transfer.
3. AI cannot settle a transaction.
4. AI cannot modify financial amounts without backend validation.
5. All financial calculations happen in backend code, never in the AI layer.
6. All state transitions go through named business commands, never generic
   field updates.
7. Every state-changing action is audited.
8. Missing information is represented explicitly (`null` + listed), never
   guessed.
9. High-confidence AI extractions still require human approval before an
   Order is created.
10. Payment and transfer operations are idempotent.

---

## 14. Compliance Note

Because this involves securities transactions and payment handling, validate
the final workflow (KYC/AML, record-keeping, applicable securities and tax
rules) with someone qualified before going live — this plan covers the
technical architecture only.
