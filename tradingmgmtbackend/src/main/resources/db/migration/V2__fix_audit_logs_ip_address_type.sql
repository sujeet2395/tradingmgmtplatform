-- V2__fix_audit_logs_ip_address_type.sql
-- Aligns audit_logs.ip_address with the entity mapping (String / varchar),
-- which is simpler to work with from Java than Postgres's native inet type
-- (no custom Hibernate UserType needed to read/write it).

ALTER TABLE audit_logs
    ALTER COLUMN ip_address TYPE VARCHAR(50) USING ip_address::varchar(50);
