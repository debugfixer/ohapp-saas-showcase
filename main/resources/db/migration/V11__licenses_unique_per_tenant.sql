-- V11__licenses_unique_per_tenant.sql
ALTER TABLE licenses
    ADD CONSTRAINT uq_licenses_tenant UNIQUE (tenant_id);
