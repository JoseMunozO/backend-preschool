ALTER TABLE payments
    ADD COLUMN receipt_file_name VARCHAR(255) NULL AFTER notes;
