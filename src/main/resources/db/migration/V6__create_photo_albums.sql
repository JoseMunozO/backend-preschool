CREATE TABLE photo_albums (
    photo_album_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NULL,
    group_id BIGINT NULL,
    student_id BIGINT NULL,
    created_by_user_id BIGINT NOT NULL,
    event_date DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_photo_albums_group
        FOREIGN KEY (group_id) REFERENCES class_groups(group_id),
    CONSTRAINT fk_photo_albums_student
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_photo_albums_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
);

CREATE TABLE photo_album_photos (
    photo_album_photo_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    photo_album_id BIGINT NOT NULL,
    student_id BIGINT NULL,
    uploaded_by_user_id BIGINT NOT NULL,
    photo_url VARCHAR(500) NOT NULL,
    caption VARCHAR(500) NULL,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_photo_album_photos_album
        FOREIGN KEY (photo_album_id) REFERENCES photo_albums(photo_album_id),
    CONSTRAINT fk_photo_album_photos_student
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_photo_album_photos_uploaded_by_user
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users(user_id)
);
