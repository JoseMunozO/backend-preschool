ALTER TABLE parents
    ADD COLUMN archived_at TIMESTAMP NULL AFTER deleted_at;
