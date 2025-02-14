ALTER TABLE iso_metadata ADD COLUMN responsible_user_id TEXT;
ALTER TABLE iso_metadata ADD COLUMN responsible_role TEXT;

ALTER TABLE client_metadata ADD COLUMN responsible_user_id TEXT;
ALTER TABLE client_metadata ADD COLUMN responsible_role TEXT;

ALTER TABLE technical_metadata ADD COLUMN responsible_user_id TEXT;
ALTER TABLE technical_metadata ADD COLUMN responsible_role TEXT;
