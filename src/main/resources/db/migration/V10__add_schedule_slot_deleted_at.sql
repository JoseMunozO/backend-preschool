ALTER TABLE schedule_slots
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER notes;
