ALTER TABLE materials
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER notes;
