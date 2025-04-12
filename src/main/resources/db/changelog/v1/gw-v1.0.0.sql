--liquibase formatted sql
--changeset Samuel Chan:gw-v1.0.0
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;

CREATE TABLE IF NOT EXISTS vector_store (
                                            id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
                                            content text,
                                            metadata json,
                                            embedding vector(1024)
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
--rollback;