-- =====================================================================
-- Trading Management Platform — Initial Schema
-- Flyway migration: V1__init_schema.sql
-- Target: PostgreSQL 14+
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- for gen_random_uuid()

-- ---------------------------------------------------------------------
-- ENUM TYPES
-- ---------------------------------------------------------------------

CREATE TYPE user_role AS ENUM
    ('ADMIN', 'OPERATIONS', 'DEALER', 'COMPLIANCE', 'FINANCE', 'VIEWER');

CREATE TYPE order_type AS ENUM ('BUY', 'SELL');

CREATE TYPE order_status AS ENUM
    ('DRAFT', 'ORDER_RECEIVED', 'ORDER_VERIFIED', 'ORDER_CONFIRMED',
     'ORDER_CANCELLED', 'ORDER_REJECTED');

CREATE TYPE transaction_status AS ENUM
    ('DRAFT', 'ORDER_RECEIVED', 'ORDER_VERIFIED', 'ORDER_CONFIRMED',
     'PAYMENT_PENDING', 'PAYMENT_RECEIVED', 'PAYMENT_VERIFIED',
     'PAYMENT_FAILED', 'TRANSFER_PENDING', 'TRANSFER_INITIATED',
     'TRANSFER_COMPLETED', 'TRANSFER_REJECTED', 'SETTLED', 'CANCELLED');

CREATE TYPE transaction_party_role AS ENUM ('BUYER', 'SELLER', 'BROKER');

CREATE TYPE payment_method AS ENUM ('CHEQUE', 'BANK_TRANSFER', 'OTHER');

CREATE TYPE payment_status AS ENUM
    ('PAYMENT_PENDING', 'CHEQUE_RECEIVED', 'CHEQUE_DEPOSITED',
     'CHEQUE_CLEARED', 'PAYMENT_CONFIRMED', 'PAYMENT_FAILED',
     'PAYMENT_CANCELLED');

CREATE TYPE payment_event_type AS ENUM
    ('CHEQUE_RECEIVED', 'CHEQUE_DEPOSITED', 'EVIDENCE_UPLOADED',
     'CHEQUE_CLEARED', 'PAYMENT_VERIFIED', 'PAYMENT_FAILED',
     'PAYMENT_CANCELLED', 'AI_PAYMENT_CLAIMED');

CREATE TYPE transfer_status AS ENUM
    ('TRANSFER_PENDING', 'DOCUMENTS_PENDING', 'DOCUMENTS_VERIFIED',
     'TRANSFER_INITIATED', 'TRANSFER_SUBMITTED', 'TRANSFER_IN_PROCESS',
     'TRANSFER_COMPLETED', 'TRANSFER_REJECTED');

CREATE TYPE transfer_event_type AS ENUM
    ('DOCUMENTS_UPLOADED', 'DOCUMENTS_VERIFIED', 'TRANSFER_INITIATED',
     'TRANSFER_SUBMITTED', 'TRANSFER_COMPLETED', 'TRANSFER_REJECTED',
     'AI_TRANSFER_CLAIMED');

CREATE TYPE chat_channel AS ENUM
    ('WEB_CHAT', 'WHATSAPP', 'EMAIL', 'TELEGRAM', 'INTERNAL');

CREATE TYPE message_sender_type AS ENUM ('CUSTOMER', 'OPERATOR', 'SYSTEM');

CREATE TYPE ai_review_status AS ENUM
    ('PENDING_REVIEW', 'APPROVED', 'EDITED', 'REJECTED');

CREATE TYPE document_category AS ENUM
    ('KYC', 'CHEQUE_EVIDENCE', 'PAYMENT_PROOF', 'TRANSFER_FORM',
     'TRANSFER_CONFIRMATION', 'AGREEMENT', 'OTHER');

CREATE TYPE notification_type AS ENUM
    ('NEW_ORDER', 'MISSING_ORDER_INFO', 'PAYMENT_PENDING',
     'CHEQUE_RECEIVED', 'CHEQUE_VERIFICATION_REQUIRED', 'PAYMENT_VERIFIED',
     'TRANSFER_DOCUMENTS_PENDING', 'TRANSFER_INITIATED',
     'TRANSFER_COMPLETED', 'TRANSACTION_REJECTED');

-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    version         INTEGER NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------
-- CUSTOMERS (buyer / seller / broker — KYC subject)
-- ---------------------------------------------------------------------

CREATE TABLE customers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name           VARCHAR(255) NOT NULL,
    pan_encrypted       BYTEA,
    phone               VARCHAR(50),
    email               VARCHAR(255),
    address             TEXT,
    bank_account_name   VARCHAR(255),
    bank_account_number_encrypted BYTEA,
    bank_ifsc           VARCHAR(20),
    kyc_verified        BOOLEAN NOT NULL DEFAULT FALSE,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID REFERENCES users(id),
    updated_by          UUID REFERENCES users(id),
    version             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_customers_full_name ON customers (full_name);

-- ---------------------------------------------------------------------
-- COMPANIES / SECURITIES
-- ---------------------------------------------------------------------

CREATE TABLE companies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name      VARCHAR(255) NOT NULL,
    cin             VARCHAR(50),
    status          VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    contact_info    JSONB,
    transfer_requirements JSONB, -- configurable per-company transfer rules
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    version         INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_companies_legal_name ON companies (legal_name);

CREATE TABLE securities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id),
    security_type   VARCHAR(50) NOT NULL DEFAULT 'EQUITY',
    isin            VARCHAR(20),
    face_value      NUMERIC(18,4),
    share_class     VARCHAR(50),
    reference_price NUMERIC(18,4),
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    version         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_securities_company_id ON securities (company_id);

-- ---------------------------------------------------------------------
-- ORDERS
-- ---------------------------------------------------------------------

CREATE TABLE orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number        VARCHAR(50) NOT NULL UNIQUE,
    order_type          order_type NOT NULL,
    buyer_customer_id   UUID REFERENCES customers(id),
    seller_customer_id  UUID REFERENCES customers(id),
    security_id         UUID NOT NULL REFERENCES securities(id),
    quantity             NUMERIC(18,4) NOT NULL CHECK (quantity > 0),
    price_per_share      NUMERIC(18,4) NOT NULL CHECK (price_per_share > 0),
    gross_amount          NUMERIC(18,4) NOT NULL CHECK (gross_amount > 0),
    currency             VARCHAR(10) NOT NULL DEFAULT 'INR',
    order_date            DATE NOT NULL DEFAULT CURRENT_DATE,
    status               order_status NOT NULL DEFAULT 'DRAFT',
    source_chat_conversation_id UUID, -- FK added after chat_conversations exists
    notes                 TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID REFERENCES users(id),
    updated_by            UUID REFERENCES users(id),
    version               INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_security_id ON orders (security_id);
CREATE INDEX idx_orders_buyer ON orders (buyer_customer_id);
CREATE INDEX idx_orders_seller ON orders (seller_customer_id);

-- ---------------------------------------------------------------------
-- TRANSACTIONS
-- ---------------------------------------------------------------------

CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_number  VARCHAR(50) NOT NULL UNIQUE,
    order_id            UUID NOT NULL REFERENCES orders(id),
    security_id         UUID NOT NULL REFERENCES securities(id),
    quantity            NUMERIC(18,4) NOT NULL CHECK (quantity > 0),
    agreed_price         NUMERIC(18,4) NOT NULL CHECK (agreed_price > 0),
    total_amount          NUMERIC(18,4) NOT NULL CHECK (total_amount > 0),
    status               transaction_status NOT NULL DEFAULT 'DRAFT',
    settled_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID REFERENCES users(id),
    updated_by            UUID REFERENCES users(id),
    version               INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_transactions_order_id ON transactions (order_id);
CREATE INDEX idx_transactions_status ON transactions (status);

CREATE TABLE transaction_parties (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    customer_id     UUID NOT NULL REFERENCES customers(id),
    role            transaction_party_role NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (transaction_id, customer_id, role)
);

CREATE INDEX idx_txn_parties_txn_id ON transaction_parties (transaction_id);

-- ---------------------------------------------------------------------
-- PAYMENTS
-- ---------------------------------------------------------------------

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID NOT NULL REFERENCES transactions(id),
    amount               NUMERIC(18,4) NOT NULL CHECK (amount > 0),
    payment_method       payment_method NOT NULL DEFAULT 'CHEQUE',
    cheque_number        VARCHAR(50),
    bank_name            VARCHAR(255),
    cheque_date           DATE,
    deposit_date           DATE,
    clearance_date          DATE,
    confirmation_date        DATE,
    status                payment_status NOT NULL DEFAULT 'PAYMENT_PENDING',
    idempotency_key         VARCHAR(100) UNIQUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             UUID REFERENCES users(id),
    updated_by             UUID REFERENCES users(id),
    version                INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE payment_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    event_type      payment_event_type NOT NULL,
    event_data      JSONB,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users(id) -- NULL when raised by the AI service
);

CREATE INDEX idx_payment_events_payment_id ON payment_events (payment_id);

-- ---------------------------------------------------------------------
-- SHARE TRANSFERS
-- ---------------------------------------------------------------------

CREATE TABLE share_transfers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID NOT NULL REFERENCES transactions(id),
    buyer_customer_id   UUID NOT NULL REFERENCES customers(id),
    seller_customer_id  UUID NOT NULL REFERENCES customers(id),
    security_id         UUID NOT NULL REFERENCES securities(id),
    quantity             NUMERIC(18,4) NOT NULL CHECK (quantity > 0),
    status               transfer_status NOT NULL DEFAULT 'TRANSFER_PENDING',
    initiated_date         DATE,
    submitted_date          DATE,
    completed_date            DATE,
    rejection_reason           TEXT,
    idempotency_key             VARCHAR(100) UNIQUE,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                 UUID REFERENCES users(id),
    updated_by                 UUID REFERENCES users(id),
    version                    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_transfers_transaction_id ON share_transfers (transaction_id);
CREATE INDEX idx_transfers_status ON share_transfers (status);

CREATE TABLE transfer_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id     UUID NOT NULL REFERENCES share_transfers(id) ON DELETE CASCADE,
    event_type      transfer_event_type NOT NULL,
    event_data      JSONB,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users(id)
);

CREATE INDEX idx_transfer_events_transfer_id ON transfer_events (transfer_id);

-- ---------------------------------------------------------------------
-- CHAT
-- ---------------------------------------------------------------------

CREATE TABLE chat_conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID REFERENCES customers(id),
    channel         chat_channel NOT NULL DEFAULT 'WEB_CHAT',
    external_ref    VARCHAR(255), -- e.g. WhatsApp thread id
    order_id        UUID REFERENCES orders(id), -- nullable: set once linked
    is_closed       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_conversations_customer_id ON chat_conversations (customer_id);
CREATE INDEX idx_chat_conversations_order_id ON chat_conversations (order_id);

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_source_conversation
    FOREIGN KEY (source_chat_conversation_id) REFERENCES chat_conversations(id);

CREATE TABLE chat_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id     UUID NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender_type         message_sender_type NOT NULL,
    sender_user_id      UUID REFERENCES users(id), -- set when sender_type = OPERATOR
    body                TEXT NOT NULL,
    attachment_document_id UUID, -- FK added after documents exists
    sent_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_conversation_id ON chat_messages (conversation_id, sent_at);

-- ---------------------------------------------------------------------
-- AI EXTRACTION
-- ---------------------------------------------------------------------

CREATE TABLE ai_extractions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id     UUID NOT NULL REFERENCES chat_conversations(id),
    triggering_message_id UUID REFERENCES chat_messages(id),
    raw_output          JSONB NOT NULL, -- full schema from §7.3 of the plan
    confidence           NUMERIC(4,3),
    requires_human_review BOOLEAN NOT NULL DEFAULT TRUE,
    review_status         ai_review_status NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by            UUID REFERENCES users(id),
    reviewed_at             TIMESTAMPTZ,
    resulting_order_id       UUID REFERENCES orders(id),
    model_name                VARCHAR(100),
    model_version               VARCHAR(50),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_extractions_conversation_id ON ai_extractions (conversation_id);
CREATE INDEX idx_ai_extractions_review_status ON ai_extractions (review_status);

CREATE TABLE ai_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    extraction_id   UUID NOT NULL REFERENCES ai_extractions(id) ON DELETE CASCADE,
    event_type      VARCHAR(100) NOT NULL, -- e.g. PAYMENT_CLAIMED, TRANSFER_CLAIMED
    event_data      JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_events_extraction_id ON ai_events (extraction_id);

-- ---------------------------------------------------------------------
-- DOCUMENTS
-- ---------------------------------------------------------------------

CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category        document_category NOT NULL,
    related_entity_type VARCHAR(50) NOT NULL, -- 'ORDER' | 'PAYMENT' | 'TRANSFER' | 'CUSTOMER'
    related_entity_id   UUID NOT NULL,
    current_version_id  UUID, -- FK added after document_versions exists
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID REFERENCES users(id)
);

CREATE INDEX idx_documents_related_entity ON documents (related_entity_type, related_entity_id);

CREATE TABLE document_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number  INTEGER NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    storage_path    TEXT NOT NULL,
    content_hash    VARCHAR(128),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploaded_by     UUID REFERENCES users(id),
    UNIQUE (document_id, version_number)
);

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_current_version
    FOREIGN KEY (current_version_id) REFERENCES document_versions(id);

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_attachment
    FOREIGN KEY (attachment_document_id) REFERENCES documents(id);

-- ---------------------------------------------------------------------
-- AUDIT LOG (immutable — application must never UPDATE/DELETE rows here)
-- ---------------------------------------------------------------------

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id), -- NULL for system/AI-originated entries
    action          VARCHAR(100) NOT NULL, -- e.g. 'PAYMENT_VERIFY', 'ORDER_CONFIRM'
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    old_value       JSONB,
    new_value       JSONB,
    reason          TEXT,
    source          VARCHAR(50), -- 'UI' | 'API' | 'AI_SERVICE' | 'SYSTEM'
    correlation_id  UUID,
    ip_address      INET,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, occurred_at);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);

-- ---------------------------------------------------------------------
-- NOTIFICATIONS
-- ---------------------------------------------------------------------

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL REFERENCES users(id),
    type            notification_type NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id   UUID,
    message         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at         TIMESTAMPTZ
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id, is_read);

-- =====================================================================
-- End of V1__init_schema.sql
-- =====================================================================
