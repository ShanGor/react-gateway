--liquibase formatted sql
--changeset Samuel Chan:gw-v1.0.2
--comment: add file storage table
CREATE TABLE IF NOT EXISTS conversation (
    id varchar(37) PRIMARY KEY,
    username varchar(37),
    last_sequence int,
    title varchar(255),
    create_time timestamp DEFAULT NOW(),
    update_time timestamp DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS conversation_detail (
    id varchar(37) PRIMARY KEY,
    conversation_id varchar(37),
    seq int,
    role varchar(32),
    content text,
    media jsonb,
    meta jsonb,
    create_time timestamp DEFAULT NOW(),
    update_time timestamp DEFAULT NOW()
);
create index conversation_detail_idx_ref on conversation_detail (conversation_id);
--rollback;
