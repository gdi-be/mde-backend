DROP TABLE IF EXISTS client_metadata cascade;
DROP TABLE IF EXISTS iso_metadata cascade;
DROP TABLE IF EXISTS technical_metadata cascade;
DROP TABLE IF EXISTS metadata_collection cascade;

CREATE TABLE metadata_collection (
    id BIGSERIAL PRIMARY KEY,
    metadata_id TEXT UNIQUE NOT NULL,
    responsible_user_ids TEXT[],
    responsible_role TEXT,
    client_metadata JSONB,
    iso_metadata JSONB,
    technical_metadata JSONB
);

CREATE INDEX idx_metadata_collection_metadata_id ON metadata_collection(metadata_id);
CREATE INDEX idx_metadata_collection_responsible_user_id ON metadata_collection(responsible_user_ids);
CREATE INDEX idx_metadata_collection_responsible_role ON metadata_collection(responsible_role);
CREATE INDEX idx_metadata_collection_client_metadata ON metadata_collection USING GIN (client_metadata);
CREATE INDEX idx_metadata_collection_iso_metadata ON metadata_collection USING GIN (iso_metadata);
CREATE INDEX idx_metadata_collection_technical_metadata ON metadata_collection USING GIN (technical_metadata);
