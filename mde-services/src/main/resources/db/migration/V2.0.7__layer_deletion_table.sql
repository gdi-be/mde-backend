create table service_deletion (
  id bigserial primary key,
  metadata_id text not null,
  file_identifier text not null
);

create index on service_deletion (metadata_id);
