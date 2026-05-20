SET search_path TO asms;

-- Add columns present in the User entity but missing from the V1 schema.
-- delivery_method is NOT NULL in the entity; default 1 = Email (lowest-risk default).
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS manager                  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS temporary_password_hash  TEXT,
    ADD COLUMN IF NOT EXISTS temporary_password_expiry INTEGER,
    ADD COLUMN IF NOT EXISTS delivery_method          INTEGER NOT NULL DEFAULT 1;

-- V1 declared department as VARCHAR(255) but DepartmentConverter maps to INTEGER.
-- Convert existing string values to their enum keys, then change the column type.
ALTER TABLE users
    ALTER COLUMN department TYPE INTEGER
    USING (CASE department
        WHEN 'Finance'          THEN 1
        WHEN 'IT_Security'      THEN 2
        WHEN 'Operations'       THEN 3
        WHEN 'HR'               THEN 4
        WHEN 'Compliance'       THEN 5
        WHEN 'Engineering'      THEN 6
        WHEN 'Customer_Support' THEN 7
        ELSE NULL
    END);
