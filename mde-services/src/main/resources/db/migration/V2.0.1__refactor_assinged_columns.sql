ALTER TABLE metadata_collection ADD COLUMN assigned_user_id TEXT;
ALTER TABLE metadata_collection RENAME COLUMN responsible_user_ids TO team_member_ids;
