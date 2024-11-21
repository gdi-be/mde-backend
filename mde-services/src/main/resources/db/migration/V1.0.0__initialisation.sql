drop table if exists client_metadata cascade;
drop table if exists iso_metadata cascade;
drop table if exists technical_metadata cascade;

create table client_metadata(id bigserial primary key, title text, metadata_id text, data jsonb);
create table iso_metadata(id bigserial primary key, title text, metadata_id text, data jsonb);
create table technical_metadata(id bigserial primary key, title text, metadata_id text, data jsonb);

create index on client_metadata(metadata_id);
create index on iso_metadata(metadata_id);
create index on technical_metadata(metadata_id);
