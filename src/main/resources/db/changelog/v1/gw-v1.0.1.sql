--liquibase formatted sql
--changeset Samuel Chan:gw-v1.0.1
--comment: add file storage table
CREATE TABLE IF NOT EXISTS upload_file_record (
    id varchar(255) PRIMARY KEY,
    file_name varchar(255) NOT NULL,
    ocr bool,
    file_path text,
    file_size bigint,
    file_type varchar(32) ,
    process_status varchar(32),
    file_checksum varchar(1024),
    create_time timestamp DEFAULT NOW(),
    update_time timestamp DEFAULT NOW()
);
create unique index upload_file_record_idx_checksum on upload_file_record (file_checksum, file_size);
--rollback;