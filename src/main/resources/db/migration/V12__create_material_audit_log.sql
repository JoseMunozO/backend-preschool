CREATE TABLE material_audit_log (
    material_audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    changed_by_user_id BIGINT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_values TEXT NOT NULL,
    new_values TEXT NOT NULL,
    CONSTRAINT fk_material_audit_log_material
        FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_material_audit_log_changed_by
        FOREIGN KEY (changed_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
