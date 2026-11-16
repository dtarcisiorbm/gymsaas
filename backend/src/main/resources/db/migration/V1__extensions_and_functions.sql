-- V1: extensions, UUID v7 generator and RLS helper functions.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- UUID v7 (RFC 9562): 48-bit Unix timestamp in ms + 74 random bits.
CREATE OR REPLACE FUNCTION uuid_generate_v7() RETURNS uuid
LANGUAGE plpgsql
VOLATILE
AS $$
DECLARE
    ts_bytes bytea;
    raw_bytes bytea;
BEGIN
    ts_bytes := substring(int8send((extract(epoch FROM clock_timestamp()) * 1000)::bigint) FROM 3);
    raw_bytes := ts_bytes || gen_random_bytes(10);
    -- version 7
    raw_bytes := set_byte(raw_bytes, 6, (get_byte(raw_bytes, 6) & 15) | 112);
    -- RFC 4122 variant (10xx)
    raw_bytes := set_byte(raw_bytes, 8, (get_byte(raw_bytes, 8) & 63) | 128);
    RETURN encode(raw_bytes, 'hex')::uuid;
END;
$$;

CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS uuid
LANGUAGE sql
STABLE
AS $$
    SELECT NULLIF(current_setting('app.current_tenant', true), '')::uuid
$$;

CREATE OR REPLACE FUNCTION current_user_id() RETURNS uuid
LANGUAGE sql
STABLE
AS $$
    SELECT NULLIF(current_setting('app.current_user', true), '')::uuid
$$;

CREATE OR REPLACE FUNCTION current_rls_bypass() RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), 'false')::boolean
$$;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;
