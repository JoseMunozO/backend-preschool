CREATE TABLE student_attendance (
    student_attendance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    recorded_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_attendance_student_date UNIQUE (student_id, attendance_date),
    CONSTRAINT fk_student_attendance_student
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_student_attendance_recorded_by_user
        FOREIGN KEY (recorded_by_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_student_attendance_date ON student_attendance(attendance_date);
