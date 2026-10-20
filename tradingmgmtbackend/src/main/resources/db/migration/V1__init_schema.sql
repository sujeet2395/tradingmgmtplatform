-- =====================================================================
-- Trading Management Platform — Phase 1 schema (core transaction engine)
-- Matches the JPA entities under com.learn.tradingmgmt.entity exactly.
-- Enums are stored as VARCHAR + CHECK constraints (not native Postgres
-- ENUM types) so they map directly via @Enumerated(EnumType.STRING)
-- without extra Hibernate configuration.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(30) NOT NULL
        CHECK (role IN ('ADMIN','OPERATIONS','DEALER','COMPLIANCE','FINANCE','VIEWER')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         INTEGER NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name                       VARCHAR(255) NOT NULL,
    pan_encrypted                   BYTEA,
    phone                           VARCHAR(50),
    email                           VARCHAR(255),
    address                         TEXT,
    bank_account_name               VARCHAR(255),
    bank_account_number_encrypted   BYTEA,
    bank_ifsc                       VARCHAR(20),
    kyc_verified                    BOOLEAN NOT NULL DEFAULT FALSE,
    notes                           TEXT,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                      UUID,
    updated_by                      UUID,
    version                         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_customers_full_name ON customers (full_name);

-- ---------------------------------------------------------------------
CREATE TABLE companies (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name              VARCHAR(255) NOT NULL UNIQUE,
    cin                     VARCHAR(50),
    status                  VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    contact_info            TEXT,
    transfer_requirements   TEXT,
    metadata                TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              UUID,
    updated_by              UUID,
    version                 INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE securities (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES companies(id),
    security_type     VARCHAR(50) NOT NULL DEFAULT 'EQUITY',
    isin              VARCHAR(20),
    face_value        NUMERIC(18,4),
    share_class       VARCHAR(50),
    reference_price   NUMERIC(18,4),
    metadata          TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    version           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_securities_company_id ON securities (company_id);

-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number        VARCHAR(50) NOT NULL UNIQUE,
    order_type          VARCHAR(10) NOT NULL CHECK (order_type IN ('BUY','SELL')),
    buyer_customer_id   UUID REFERENCES customers(id),
    seller_customer_id  UUID REFERENCES customers(id),
    security_id         UUID NOT NULL REFERENCES securities(id),
    quantity            NUMERIC(18,4) NOT NULL CHECK (quantity > 0),
    price_per_share     NUMERIC(18,4) NOT NULL CHECK (price_per_share > 0),
    gross_amount        NUMERIC(18,4) NOT NULL CHECK (gross_amount > 0),
    currency            VARCHAR(10) NOT NULL DEFAULT 'INR',
    order_date          DATE NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','ORDER_RECEIVED','ORDER_VERIFIED','ORDER_CONFIRMED','ORDER_CANCELLED','ORDER_REJECTED')),
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_security_id ON orders (security_id);
CREATE INDEX idx_orders_buyer ON orders (buyer_customer_id);
CREATE INDEX idx_orders_seller ON orders (seller_customer_id);

-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_number   VARCHAR(50) NOT NULL UNIQUE,
    order_id             UUID NOT NULL REFERENCES orders(id),
    security_id          UUID NOT NULL REFERENCES securities(id),
    quantity             NUMERIC(18,4) NOT NULL CHECK (quantity > 0),
    agreed_price         NUMERIC(18,4) NOT NULL CHECK (agreed_price > 0),
    total_amount         NUMERIC(18,4) NOT NULL CHECK (total_amount > 0),
    status               VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT','ORDER_RECEIVED','ORDER_VERIFIED','ORDER_CONFIRMED',
        'PAYMENT_PENDING','PAYMENT_RECEIVED','PAYMENT_VERIFIED','PAYMENT_FAILED',
        'TRANSFER_PENDING','TRANSFER_INITIATED','TRANSFER_COMPLETED','TRANSFER_REJECTED',
        'SETTLED','CANCELLED')),
    settled_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_by           UUID,
    version              INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_transactions_order_id ON transactions (order_id);
CREATE INDEX idx_transactions_status ON transactions (status);

CREATE TABLE transaction_parties (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    customer_id     UUID NOT NULL REFERENCES customers(id),
    role            VARCHAR(10) NOT NULL CHECK (role IN ('BUYER','SELLER','BROKER')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (transaction_id, customer_id, role)
);

CREATE INDEX idx_txn_parties_txn_id ON transaction_parties (transaction_id);

-- ---------------------------------------------------------------------
CREATE TABLE payments (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id       UUID NOT NULL REFERENCES transactions(id),
    amount               NUMERIC(18,4) NOT NULL CHECK (amount > 0),
    payment_method       VARCHAR(20) NOT NULL DEFAULT 'CHEQUE' CHECK (payment_method IN ('CHEQUE','BANK_TRANSFER','OTHER')),
    cheque_number        VARCHAR(50),
    bank_name            VARCHAR(255),
    cheque_date          DATE,
    deposit_date         DATE,
    clearance_date       DATE,
    confirmation_date    DATE,
    status               VARCHAR(30) NOT NULL DEFAULT 'PAYMENT_PENDING' CHECK (status IN (
        'PAYMENT_PENDING','CHEQUE_RECEIVED','CHEQUE_DEPOSITED','CHEQUE_CLEARED',
        'PAYMENT_CONFIRMED','PAYMENT_FAILED','PAYMENT_CANCELLED')),
    idempotency_key      VARCHAR(100) UNIQUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_by           UUID,
    version              INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE payment_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    event_type      VARCHAR(30) NOT NULL CHECK (event_type IN (
        'CHEQUE_RECEIVED','CHEQUE_DEPOSITED','EVIDENCE_UPLOADED','CHEQUE_CLEARED',
        'PAYMENT_VERIFIED','PAYMENT_FAILED','PAYMENT_CANCELLED')),
    event_data      TEXT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID
);

CREATE INDEX idx_payment_events_payment_id ON payment_events (payment_id);

-- ---------------------------------------------------------------------
CREATE TABLE share_transfers (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id       UUID NOT NULL REFERENCES transactions(id),
    buyer_customer_id    UUID NOT NULL REFERENCES customers(id),
    seller_customer_id   UUID NOT NULL REFERENCES customers(id),
    security_id          UUID NOT NULL REFERENCES securities(id),
    quantity             NUMERIC(18,4) NOT NULL CHECK (quantity > 0),
    status               VARCHAR(30) NOT NULL DEFAULT 'TRANSFER_PENDING' CHECK (status IN (
        'TRANSFER_PENDING','DOCUMENTS_PENDING','DOCUMENTS_VERIFIED','TRANSFER_INITIATED',
        'TRANSFER_SUBMITTED','TRANSFER_IN_PROCESS','TRANSFER_COMPLETED','TRANSFER_REJECTED')),
    initiated_date       DATE,
    submitted_date       DATE,
    completed_date       DATE,
    rejection_reason     TEXT,
    idempotency_key      VARCHAR(100) UNIQUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_by           UUID,
    version              INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_transfers_transaction_id ON share_transfers (transaction_id);
CREATE INDEX idx_transfers_status ON share_transfers (status);

CREATE TABLE transfer_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id     UUID NOT NULL REFERENCES share_transfers(id) ON DELETE CASCADE,
    event_type      VARCHAR(30) NOT NULL CHECK (event_type IN (
        'DOCUMENTS_UPLOADED','DOCUMENTS_VERIFIED','TRANSFER_INITIATED',
        'TRANSFER_SUBMITTED','TRANSFER_COMPLETED','TRANSFER_REJECTED')),
    event_data      TEXT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID
);

CREATE INDEX idx_transfer_events_transfer_id ON transfer_events (transfer_id);

-- ---------------------------------------------------------------------
CREATE TABLE document_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL, -- FK added below, after documents exists
    version_number  INTEGER NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    storage_path    TEXT NOT NULL,
    content_hash    VARCHAR(128),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploaded_by     UUID,
    UNIQUE (document_id, version_number)
);

CREATE TABLE documents (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category              VARCHAR(30) NOT NULL CHECK (category IN (
        'KYC','CHEQUE_EVIDENCE','PAYMENT_PROOF','TRANSFER_FORM',
        'TRANSFER_CONFIRMATION','AGREEMENT','OTHER')),
    related_entity_type   VARCHAR(50) NOT NULL,
    related_entity_id     UUID NOT NULL,
    current_version_id    UUID REFERENCES document_versions(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID
);

ALTER TABLE document_versions
    ADD CONSTRAINT fk_document_versions_document
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

CREATE INDEX idx_documents_related_entity ON documents (related_entity_type, related_entity_id);

-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    old_value       TEXT,
    new_value       TEXT,
    reason          TEXT,
    source          VARCHAR(50),
    correlation_id  UUID,
    ip_address      VARCHAR(50),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, occurred_at);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);

-- =====================================================================
-- End of V1__init_schema.sql (Phase 1)
-- =====================================================================

-- some fix
-- 1. Alter the column type using a custom cast expression
ALTER TABLE customers 
ALTER COLUMN bank_account_number_encrypted TYPE oid USING lo_from_bytea(0, bank_account_number_encrypted);
ALTER TABLE customers 
ALTER COLUMN pan_encrypted TYPE oid USING lo_from_bytea(0, pan_encrypted);