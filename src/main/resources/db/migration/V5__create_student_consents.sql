CREATE TABLE student_consents (
    student_consent_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    recorded_by_user_id BIGINT NOT NULL,
    consent_type VARCHAR(40) NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT NULL,
    accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_consents_student
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_student_consents_parent
        FOREIGN KEY (parent_id) REFERENCES parents(parent_id),
    CONSTRAINT fk_student_consents_recorded_by_user
        FOREIGN KEY (recorded_by_user_id) REFERENCES users(user_id)
);
