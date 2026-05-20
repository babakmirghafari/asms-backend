-- Align all enum-converter columns from SMALLINT to INTEGER.
-- AbstractEnumConverter maps to Integer (Types#INTEGER); schema validation requires INTEGER.
ALTER TABLE organizations ALTER COLUMN status TYPE INTEGER;
ALTER TABLE applications ALTER COLUMN type TYPE INTEGER;
ALTER TABLE applications ALTER COLUMN status TYPE INTEGER;
ALTER TABLE applications ALTER COLUMN integration_health_status TYPE INTEGER;
ALTER TABLE station_policies ALTER COLUMN status TYPE INTEGER;
ALTER TABLE alerts ALTER COLUMN risk_level TYPE INTEGER;
