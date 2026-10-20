ALTER TABLE document_versions
    ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED')),
    ADD COLUMN verified_by UUID,
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD COLUMN verification_note TEXT;