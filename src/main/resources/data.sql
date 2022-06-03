
create table if not exists d_upload_record
(
    id bigint not null,
    id_task varchar(36) not null,
    file_name varchar(255) not null,
    file_size bigint not null,
    upload_time timestamp not null,
    target varchar(64) not null,
    bucket_name varchar(64) not null,
    primary key (id)
);

create table if not exists d_upload_slice
(
    id bigint not null,
    id_upload_record bigint not null,
    file_name varchar(255) not null,
    file_size bigint not null,
    primary key (id)
);
