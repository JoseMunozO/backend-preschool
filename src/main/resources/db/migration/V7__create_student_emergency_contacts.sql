CREATE TABLE student_emergency_contacts (
    student_emergency_contact_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    alternate_phone VARCHAR(30) NULL,
    notes TEXT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_emergency_contacts_student
        FOREIGN KEY (student_id) REFERENCES students(student_id)
);
