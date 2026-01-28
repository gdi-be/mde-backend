-- Migration: Move privacy field from clientMetadata to isoMetadata
-- Version: V2.0.8__move_privacy_to_iso_metadata.sql

-- Update all metadata_collection entries to move the privacy field
UPDATE metadata_collection
SET
  iso_metadata = jsonb_set(
    COALESCE(iso_metadata, '{}'::jsonb),
    '{privacy}',
    client_metadata->'privacy',
    true
  ),
  client_metadata = client_metadata - 'privacy'
WHERE
  client_metadata ? 'privacy'
    AND client_metadata->'privacy' IS NOT NULL;
