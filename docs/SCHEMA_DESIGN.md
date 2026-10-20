# Trading Management Platform — Schema Design

> Companion documentation for `V1__init_schema.sql`. Describes the entity
> relationships, design decisions, and how each table maps back to the
> business plan (`TRADING_MANAGEMENT_PLATFORM.md`).

---

## 1. Entity-Relationship Overview

```
companies ──1:N── securities
                       │
                       │ N:1
                       ▼
customers ──1:N── orders ──1:N── transactions ──1:1── transaction_parties (N)
   (buyer/seller)         │                │
                          │                ├──1:N── payments ──1:N── payment_events
                          │                │
                          │                └──1:N── share_transfers ──1:N── transfer_events
                          │
                          ▼
             chat_conversations ──1:N── chat_messages
                          │
                          ▼
                  ai_extractions ──1:N── ai_events
                (may result in an order)

documents ──1:N── document_versions
   (attached to orders / payments / transfers / customers via
    related_entity_type + related_entity_id)

audit_logs        — one row per state-changing action, references any entity
notifications     — per-user, references any entity
```

---

## 2. Table-by-Table Notes

### `users`
Backend operators (not buyers/sellers). `role` drives authorization
(`ADMIN`, `OPERATIONS`, `DEALER`, `COMPLIANCE`, `FINANCE`, `VIEWER`) per §11
of the business plan — only `FINANCE` should be allowed to call
`/payments/{id}/verify`, enforced at the service layer, not just the schema.

### `customers`
Buyers, sellers, and brokers share one table with a role assigned per
transaction (see `transaction_parties`), since the same person/entity can be
a buyer in one deal and a seller in another. PAN and bank account number are
stored as `BYTEA` — encrypt at the application layer before insert (e.g.
via Spring's `AttributeConverter` + AES) rather than relying on Postgres
alone.

### `companies` / `securities`
Kept separate because a company can (eventually) have more than one class of
security. `transfer_requirements JSONB` on `companies` is intentionally
schema-less — this is where company-specific transfer rules (documents
required, turnaround time, etc.) live without needing a migration every time
a new company is onboarded.

### `orders`
Represents *intention*, not settlement (§4 of the plan). `status` is a
narrower enum than `transactions.status` — an order only needs to reach
`ORDER_CONFIRMED` before a `transaction` row is created. `source_chat_conversation_id`
links back to the conversation that produced it, for traceability into the
AI extraction that generated it.

### `transactions` / `transaction_parties`
One order can in principle produce more than one transaction (e.g. partial
fills) — kept as a separate table rather than folding settlement fields into
`orders`. `transaction_parties` normalizes buyer/seller/broker into rows
instead of fixed columns, so a broker can be added later without a schema
change.

### `payments` / `payment_events`
`payments.status` is the **current** state; `payment_events` is the
append-only history that produced it (mirrors §6.2 of the plan). Application
code should never `UPDATE payments SET status = ...` directly — it should
insert a `payment_events` row and derive/update `payments.status` in the
same transaction, so the two never drift apart.  `idempotency_key` prevents
a retried "mark cheque received" call from creating a duplicate payment.

### `share_transfers` / `transfer_events`
Mirrors the payments pattern for the transfer side of settlement (§6.3).

### `chat_conversations` / `chat_messages`
`channel` supports the multi-channel design from §5/§15 of the plan (web
chat now, WhatsApp later) without a schema change — WhatsApp threads map in
via `external_ref`. A conversation may exist with `order_id IS NULL` (not
yet resulted in an order) or be linked once one is created.

### `ai_extractions` / `ai_events`
`raw_output` stores the full JSON payload from the AI service verbatim
(§7.3 schema) so nothing is lost even if the reviewer edits fields before
approval. `review_status` enforces the human-in-the-loop rule from §7.4 —
`resulting_order_id` is only populated after a reviewer approves and an
order is actually created; it is never set by the AI service itself.
`ai_events` holds the individual claims (`PAYMENT_CLAIMED`,
`TRANSFER_CLAIMED`) distinct from the backend-owned `payment_events` /
`transfer_events` tables — this keeps "what the customer claimed" separate
from "what was actually verified," per the plan's core rule.

### `documents` / `document_versions`
Polymorphic association (`related_entity_type` + `related_entity_id`)
rather than a nullable FK per entity type, since the same document model
covers KYC, cheque evidence, transfer forms, etc. Versions are append-only —
`documents.current_version_id` just points at the latest; older rows in
`document_versions` are never deleted, satisfying the "don't overwrite
important historical documents" rule (§10 of the plan).

### `audit_logs`
Deliberately not tied to a single parent table via FK — `entity_type` +
`entity_id` is generic so any table can be audited without a schema change.
Application code must treat this table as insert-only.

### `notifications`
Simple per-user inbox; `related_entity_type`/`related_entity_id` lets the
UI deep-link a notification to the order/payment/transfer it's about.

---

## 3. Cross-Cutting Conventions (applied throughout)

- **UUID primary keys** (`gen_random_uuid()`) everywhere — avoids leaking
  sequential IDs and simplifies merging data from the AI service later.
- **`created_at`/`updated_at`/`created_by`/`updated_by`** on every mutable
  table, `version` for optimistic locking (maps to JPA `@Version`) on the
  financially significant tables (`orders`, `transactions`, `payments`,
  `share_transfers`, `customers`, `companies`, `securities`, `users`).
- **No hard deletes** on financial tables — add an `is_archived`/soft-delete
  column when that need arises rather than allowing `DELETE`.
- **Enums as Postgres `ENUM` types**, matching the state machines in §6 of
  the plan 1:1, so an invalid status value can't be inserted even by a bug
  that bypasses the service layer.
- **Event tables are append-only** (`payment_events`, `transfer_events`,
  `ai_events`, `audit_logs`) — the current-state columns on the parent table
  are a derived/cached view of the latest event, never the other way round.

---

## 4. What's Deliberately Left Out of V1

- Row-level security policies (add once role-based data partitioning
  requirements are confirmed)
- Partitioning on `audit_logs` / `chat_messages` (revisit once volume is
  known — both are natural candidates for time-based partitioning)
- Full-text search indexes on `chat_messages.body` (add via `tsvector` once
  the chat inbox search requirement is scoped)

These are intentionally deferred rather than guessed at, in line with the
phased delivery plan (§12) — Phase 1 only needs the core transaction engine
to work end-to-end.
