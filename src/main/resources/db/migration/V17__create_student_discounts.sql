CREATE TABLE student_discounts (
    student_discount_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    reason VARCHAR(255),
    valid_from DATE NOT NULL,
    valid_until DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_discounts_student
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_student_discounts_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_student_discounts_student ON student_discounts(student_id);
