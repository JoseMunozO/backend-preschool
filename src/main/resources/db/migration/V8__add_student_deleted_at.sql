ALTER TABLE students
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER withdrawal_date;
