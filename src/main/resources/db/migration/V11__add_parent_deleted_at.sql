ALTER TABLE parents
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER notes;
