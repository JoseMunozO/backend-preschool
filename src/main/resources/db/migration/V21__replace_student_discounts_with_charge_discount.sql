ALTER TABLE student_charges
    ADD COLUMN original_amount DECIMAL(10,2) NULL,
    ADD COLUMN discount_type VARCHAR(20) NULL,
    ADD COLUMN discount_value DECIMAL(10,2) NULL,
    ADD COLUMN discount_reason VARCHAR(255) NULL;

DROP TABLE IF EXISTS student_discounts;
